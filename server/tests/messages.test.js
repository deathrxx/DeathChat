const request = require('supertest');
const createApp = require('../src/app');
const Message = require('../src/models/Message');
const Room = require('../src/models/Room');
const { setupTestDb, clearTestDb, teardownTestDb } = require('./setup');

let app;
let userToken;
let roomId;

beforeAll(async () => {
  await setupTestDb();
  app = createApp().app;
});

beforeEach(async () => {
  const s = await request(app).post('/api/sessions').send({ displayName: 'ActiveSender' });
  userToken = s.body.data.sessionId;

  const r = await request(app)
    .post('/api/rooms')
    .set('x-session-id', userToken)
    .send({ name: 'Message Test Room', retentionSeconds: 1800 });
  roomId = r.body.data.roomId;
});

afterEach(async () => {
  await clearTestDb();
});

afterAll(async () => {
  await teardownTestDb();
});

describe('Messages API and Ephemeral Expiration', () => {
  test('POST /api/rooms/:roomId/messages creates message with server-generated expiration', async () => {
    const res = await request(app)
      .post(`/api/rooms/${roomId}/messages`)
      .set('x-session-id', userToken)
      .send({ content: 'Hello DeathChat!' });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.messageId).toBeDefined();
    expect(res.body.data.content).toBe('Hello DeathChat!');
    expect(new Date(res.body.data.expiresAt).getTime()).toBeGreaterThan(Date.now());
  });

  test('GET /api/rooms/:roomId/messages returns message history in chronological order', async () => {
    await request(app)
      .post(`/api/rooms/${roomId}/messages`)
      .set('x-session-id', userToken)
      .send({ content: 'First message' });

    await request(app)
      .post(`/api/rooms/${roomId}/messages`)
      .set('x-session-id', userToken)
      .send({ content: 'Second message' });

    const res = await request(app).get(`/api/rooms/${roomId}/messages`);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBe(2);
    expect(res.body.data[0].content).toBe('First message');
    expect(res.body.data[1].content).toBe('Second message');
  });

  test('GET /api/rooms/:roomId/messages strictly excludes expired messages from history', async () => {
    // Insert an active message
    await request(app)
      .post(`/api/rooms/${roomId}/messages`)
      .set('x-session-id', userToken)
      .send({ content: 'Active message' });

    // Manually create an expired message directly in DB
    await Message.create({
      messageId: 'expired-msg-123',
      roomId,
      senderSessionId: userToken,
      senderDisplayName: 'ActiveSender',
      senderAvatar: 'avatar_reaper',
      content: 'This message should not be visible',
      createdAt: new Date(Date.now() - 3600000),
      expiresAt: new Date(Date.now() - 1000) // Expired 1 second ago
    });

    const res = await request(app).get(`/api/rooms/${roomId}/messages`);

    expect(res.status).toBe(200);
    expect(res.body.data.length).toBe(1);
    expect(res.body.data[0].content).toBe('Active message');
  });

  test('POST /api/rooms/:roomId/messages rejects sending to expired room', async () => {
    // Expire the room in the DB
    await Room.updateOne({ roomId }, { expiresAt: new Date(Date.now() - 1000) });

    const res = await request(app)
      .post(`/api/rooms/${roomId}/messages`)
      .set('x-session-id', userToken)
      .send({ content: 'Trying to send to expired room' });

    expect(res.status).toBe(404);
    expect(res.body.success).toBe(false);
  });

  test('POST /api/rooms/:roomId/messages rejects message exceeding 1000 characters', async () => {
    const longText = 'a'.repeat(1005);
    const res = await request(app)
      .post(`/api/rooms/${roomId}/messages`)
      .set('x-session-id', userToken)
      .send({ content: longText });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
  });
});
