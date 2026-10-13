const request = require('supertest');
const createApp = require('../src/app');
const { setupTestDb, clearTestDb, teardownTestDb } = require('./setup');

let app;

beforeAll(async () => {
  await setupTestDb();
  app = createApp().app;
});

afterEach(async () => {
  await clearTestDb();
});

afterAll(async () => {
  await teardownTestDb();
});

describe('Anonymous Sessions API', () => {
  test('POST /api/sessions creates an anonymous session with default random identity', async () => {
    const res = await request(app).post('/api/sessions').send({});

    expect(res.status).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data.sessionId).toBeDefined();
    expect(res.body.data.displayName).toBeDefined();
    expect(res.body.data.avatar).toBeDefined();
  });

  test('POST /api/sessions creates a session with custom display name and avatar', async () => {
    const res = await request(app)
      .post('/api/sessions')
      .send({ displayName: 'ShadowReaper_99', avatar: 'avatar_skull' });

    expect(res.status).toBe(201);
    expect(res.body.data.displayName).toBe('ShadowReaper_99');
    expect(res.body.data.avatar).toBe('avatar_skull');
  });

  test('POST /api/sessions rejects invalid display name format', async () => {
    const res = await request(app)
      .post('/api/sessions')
      .send({ displayName: '!' }); // Too short and invalid char

    expect(res.status).toBe(400);
    expect(res.body.success).toBe(false);
  });

  test('GET /api/sessions/me restores active session with valid token', async () => {
    const created = await request(app)
      .post('/api/sessions')
      .send({ displayName: 'ViperGhost' });

    const token = created.body.data.sessionId;

    const res = await request(app)
      .get('/api/sessions/me')
      .set('x-session-id', token);

    expect(res.status).toBe(200);
    expect(res.body.data.displayName).toBe('ViperGhost');
  });

  test('GET /api/sessions/me rejects invalid or non-existent session token', async () => {
    const res = await request(app)
      .get('/api/sessions/me')
      .set('x-session-id', 'non-existent-token-123');

    expect(res.status).toBe(401);
    expect(res.body.success).toBe(false);
  });
});
