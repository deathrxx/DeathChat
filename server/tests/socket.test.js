const http = require('http');
const ioClient = require('socket.io-client');
const createApp = require('../src/app');
const { initSocketServer } = require('../src/socket');
const { setupTestDb, clearTestDb, teardownTestDb } = require('./setup');
const Session = require('../src/models/Session');
const Room = require('../src/models/Room');

let server;
let io;
let serverPort;
let testSession1;
let testSession2;
let testRoom;

beforeAll(async () => {
  await setupTestDb();
  const { app, corsOptions } = createApp();
  server = http.createServer(app);
  io = initSocketServer(server, corsOptions);

  await new Promise(resolve => {
    server.listen(0, () => {
      serverPort = server.address().port;
      resolve();
    });
  });
});

beforeEach(async () => {
  testSession1 = await Session.create({
    sessionId: 'session-alpha-123',
    displayName: 'AlphaGhost',
    avatar: 'avatar_reaper'
  });

  testSession2 = await Session.create({
    sessionId: 'session-beta-456',
    displayName: 'BetaSpectre',
    avatar: 'avatar_wraith'
  });

  testRoom = await Room.create({
    roomId: 'DC-TEST-1',
    name: 'Socket Arena',
    creatorSessionId: testSession1.sessionId,
    messageRetentionSeconds: 3600,
    expiresAt: new Date(Date.now() + 86400000)
  });
});

afterEach(async () => {
  await clearTestDb();
});

afterAll(async () => {
  io.close();
  await new Promise(resolve => server.close(resolve));
  await teardownTestDb();
});

function connectSocket(token) {
  return new Promise((resolve, reject) => {
    const socket = ioClient(`http://localhost:${serverPort}`, {
      auth: { token },
      transports: ['websocket'],
      reconnection: false
    });

    socket.on('connect', () => resolve(socket));
    socket.on('connect_error', err => reject(err));
  });
}

describe('Socket.IO Real-time Messaging and Events', () => {
  test('Reject handshake without valid session token', async () => {
    await expect(connectSocket('invalid-non-existent-token')).rejects.toThrow(
      /Authentication failed/i
    );
  });

  test('Connect successfully and join room with presence tracking', async () => {
    const socket1 = await connectSocket(testSession1.sessionId);

    const joinRes = await new Promise(resolve => {
      socket1.emit('join_room', { roomId: testRoom.roomId }, resolve);
    });

    expect(joinRes.success).toBe(true);
    expect(joinRes.data.roomId).toBe(testRoom.roomId);
    expect(joinRes.data.isOwner).toBe(true);
    expect(joinRes.data.participants.length).toBe(1);

    socket1.disconnect();
  });

  test('Send and receive real-time messages between two clients', async () => {
    const socket1 = await connectSocket(testSession1.sessionId);
    const socket2 = await connectSocket(testSession2.sessionId);

    // Both join room
    await new Promise(resolve => socket1.emit('join_room', { roomId: testRoom.roomId }, resolve));
    await new Promise(resolve => socket2.emit('join_room', { roomId: testRoom.roomId }, resolve));

    // Client 2 listens for message
    const messagePromise = new Promise(resolve => {
      socket2.on('new_message', msg => resolve(msg));
    });

    // Client 1 sends message
    const sendAck = await new Promise(resolve => {
      socket1.emit(
        'send_message',
        { roomId: testRoom.roomId, content: 'Real-time greeting!' },
        resolve
      );
    });

    expect(sendAck.success).toBe(true);

    const receivedMessage = await messagePromise;
    expect(receivedMessage.content).toBe('Real-time greeting!');
    expect(receivedMessage.senderSessionId).toBe(testSession1.sessionId);

    socket1.disconnect();
    socket2.disconnect();
  });

  test('Typing indicators broadcast correctly', async () => {
    const socket1 = await connectSocket(testSession1.sessionId);
    const socket2 = await connectSocket(testSession2.sessionId);

    await new Promise(resolve => socket1.emit('join_room', { roomId: testRoom.roomId }, resolve));
    await new Promise(resolve => socket2.emit('join_room', { roomId: testRoom.roomId }, resolve));

    const typingPromise = new Promise(resolve => {
      socket2.on('typing_update', data => resolve(data));
    });

    socket1.emit('typing_start', { roomId: testRoom.roomId });

    const typingData = await typingPromise;
    expect(typingData.sessionId).toBe(testSession1.sessionId);
    expect(typingData.isTyping).toBe(true);

    socket1.disconnect();
    socket2.disconnect();
  });
});
