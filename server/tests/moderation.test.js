const request = require('supertest');
const createApp = require('../src/app');
const roomService = require('../src/services/roomService');
const messageService = require('../src/services/messageService');
const { setupTestDb, clearTestDb, teardownTestDb } = require('./setup');

let app;
let ownerToken;
let memberToken;
let roomId;

beforeAll(async () => {
  await setupTestDb();
  app = createApp().app;
});

beforeEach(async () => {
  const o = await request(app).post('/api/sessions').send({ displayName: 'RoomOwner' });
  ownerToken = o.body.data.sessionId;

  const m = await request(app).post('/api/sessions').send({ displayName: 'RoomMember' });
  memberToken = m.body.data.sessionId;

  const r = await request(app)
    .post('/api/rooms')
    .set('x-session-id', ownerToken)
    .send({ name: 'Moderated Crypt' });
  roomId = r.body.data.roomId;
});

afterEach(async () => {
  await clearTestDb();
});

afterAll(async () => {
  await teardownTestDb();
});

describe('Moderation and Permission Enforcement', () => {
  test('Owner can kick participant, and kicked participant cannot rejoin', async () => {
    // Owner kicks member
    await roomService.kickParticipant(roomId, ownerToken, memberToken);

    // Kicked participant tries to join
    const joinRes = await request(app)
      .post(`/api/rooms/${roomId}/join`)
      .set('x-session-id', memberToken)
      .send({});

    expect(joinRes.status).toBe(403);
    expect(joinRes.body.error).toMatch(/kicked/i);
  });

  test('Member cannot kick room owner', async () => {
    await expect(
      roomService.kickParticipant(roomId, memberToken, ownerToken)
    ).rejects.toThrow(/Only the room owner can remove participants/i);
  });

  test('Owner can mute participant, and muted participant cannot send messages', async () => {
    // Owner mutes member
    await roomService.muteParticipant(roomId, ownerToken, memberToken);

    // Member attempts to send message
    await expect(
      messageService.createMessage({
        roomId,
        senderSession: { sessionId: memberToken, displayName: 'RoomMember', avatar: 'avatar_reaper' },
        content: 'This message should be rejected'
      })
    ).rejects.toThrow(/muted/i);
  });

  test('Owner can close room, and non-owner cannot close room', async () => {
    // Non-owner fails
    const badRes = await request(app)
      .post(`/api/rooms/${roomId}/close`)
      .set('x-session-id', memberToken);
    expect(badRes.status).toBe(403);

    // Owner succeeds
    const okRes = await request(app)
      .post(`/api/rooms/${roomId}/close`)
      .set('x-session-id', ownerToken);
    expect(okRes.status).toBe(200);
    expect(okRes.body.data.isClosed).toBe(true);
  });

  test('Owner can delete room, and non-owner cannot delete room', async () => {
    // Non-owner fails
    const badRes = await request(app)
      .delete(`/api/rooms/${roomId}`)
      .set('x-session-id', memberToken);
    expect(badRes.status).toBe(403);

    // Owner succeeds
    const okRes = await request(app)
      .delete(`/api/rooms/${roomId}`)
      .set('x-session-id', ownerToken);
    expect(okRes.status).toBe(200);
    expect(okRes.body.success).toBe(true);

    // Verify room is gone
    const checkRes = await request(app).get(`/api/rooms/${roomId}`);
    expect(checkRes.status).toBe(404);
  });
});
