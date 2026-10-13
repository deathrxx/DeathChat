const request = require('supertest');
const createApp = require('../src/app');
const { setupTestDb, clearTestDb, teardownTestDb } = require('./setup');

let app;
let creatorToken;
let joinerToken;

beforeAll(async () => {
  await setupTestDb();
  app = createApp().app;
});

beforeEach(async () => {
  // Create test sessions
  const s1 = await request(app).post('/api/sessions').send({ displayName: 'CreatorUser' });
  creatorToken = s1.body.data.sessionId;

  const s2 = await request(app).post('/api/sessions').send({ displayName: 'JoinerUser' });
  joinerToken = s2.body.data.sessionId;
});

afterEach(async () => {
  await clearTestDb();
});

afterAll(async () => {
  await teardownTestDb();
});

describe('Rooms API', () => {
  test('POST /api/rooms creates a public room successfully', async () => {
    const res = await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({
        name: 'The Cyber Crypt',
        description: 'Anonymous underground chat',
        isPrivate: false,
        retentionSeconds: 1800
      });

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.roomId).toBeDefined();
    expect(res.body.data.isPrivate).toBe(false);
    expect(res.body.data.messageRetentionSeconds).toBe(1800);
    expect(res.body.data.passwordHash).toBeUndefined(); // Never expose passwordHash
  });

  test('POST /api/rooms creates a private room with password', async () => {
    const res = await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({
        name: 'Secret Chamber',
        isPrivate: true,
        password: 'vaultPassword123'
      });

    expect(res.status).toBe(201);
    expect(res.body.data.isPrivate).toBe(true);
    expect(res.body.data.passwordHash).toBeUndefined(); // Must not expose passwordHash
  });

  test('POST /api/rooms rejects private room without password', async () => {
    const res = await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({
        name: 'Secret Chamber',
        isPrivate: true
      });

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
  });

  test('GET /api/rooms lists public rooms and never exposes private rooms or passwords', async () => {
    // 1. Create public room
    await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({ name: 'Public Lounge', isPrivate: false });

    // 2. Create private room
    await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({ name: 'Private Secret', isPrivate: true, password: 'secretPass123' });

    const listRes = await request(app).get('/api/rooms');

    expect(listRes.status).toBe(200);
    expect(listRes.body.data.length).toBe(1);
    expect(listRes.body.data[0].name).toBe('Public Lounge');
    expect(listRes.body.data[0].passwordHash).toBeUndefined();
  });

  test('POST /api/rooms/:roomId/join allows authorized access to private room with correct password', async () => {
    const createRes = await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({ name: 'Restricted Vault', isPrivate: true, password: 'mySecurePassword' });

    const roomId = createRes.body.data.roomId;

    // Join with correct password
    const joinRes = await request(app)
      .post(`/api/rooms/${roomId}/join`)
      .set('x-session-id', joinerToken)
      .send({ password: 'mySecurePassword' });

    expect(joinRes.status).toBe(200);
    expect(joinRes.body.success).toBe(true);
  });

  test('POST /api/rooms/:roomId/join rejects private room join with wrong password', async () => {
    const createRes = await request(app)
      .post('/api/rooms')
      .set('x-session-id', creatorToken)
      .send({ name: 'Restricted Vault', isPrivate: true, password: 'mySecurePassword' });

    const roomId = createRes.body.data.roomId;

    // Join with wrong password
    const joinRes = await request(app)
      .post(`/api/rooms/${roomId}/join`)
      .set('x-session-id', joinerToken)
      .send({ password: 'wrongPassword' });

    expect(joinRes.status).toBe(401);
    expect(joinRes.body.success).toBe(false);
  });
});
