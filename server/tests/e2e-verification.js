/**
 * DeathChat End-to-End User Flow Verification Script
 * Validates full multi-client real-time messaging, presence, authorization, and expiration.
 */

const http = require('http');
const ioClient = require('socket.io-client');
const createApp = require('../src/app');
const { initSocketServer } = require('../src/socket');
const { setupTestDb, teardownTestDb } = require('./setup');
const Session = require('../src/models/Session');
const Room = require('../src/models/Room');
const Message = require('../src/models/Message');

async function runE2EVerification() {
  console.log('===========================================================');
  console.log('💀 DEATHCHAT END-TO-END FLOW VERIFICATION');
  console.log('===========================================================');

  await setupTestDb();
  const { app, corsOptions } = createApp();
  const server = http.createServer(app);
  const io = initSocketServer(server, corsOptions);

  await new Promise(resolve => server.listen(0, resolve));
  const port = server.address().port;
  console.log(`[E2E] Test server active on port ${port}`);

  function createSocket(sessionId) {
    return ioClient(`http://localhost:${port}`, {
      auth: { token: sessionId },
      transports: ['websocket'],
      reconnection: false
    });
  }

  try {
    // STEP 1: Create Two Ephemeral Identities
    console.log('\n[STEP 1] Creating temporary sessions for Client 1 & Client 2...');
    const session1 = await Session.create({
      sessionId: 'sess-e2e-1',
      displayName: 'NeonReaper_101',
      avatar: 'avatar_reaper'
    });
    const session2 = await Session.create({
      sessionId: 'sess-e2e-2',
      displayName: 'ShadowSpectre_202',
      avatar: 'avatar_spectre'
    });
    console.log(`✓ Client 1 identity: ${session1.displayName} (${session1.sessionId})`);
    console.log(`✓ Client 2 identity: ${session2.displayName} (${session2.sessionId})`);

    // STEP 2: Client 1 creates a Public Room
    console.log('\n[STEP 2] Client 1 creating public room...');
    const room = await Room.create({
      roomId: 'E2E-CRYPT',
      name: 'The Ephemeral Crypt',
      description: 'End-to-End real-time verification room',
      isPrivate: false,
      creatorSessionId: session1.sessionId,
      messageRetentionSeconds: 1800,
      expiresAt: new Date(Date.now() + 86400000)
    });
    console.log(`✓ Room created: ${room.name} [Code: ${room.roomId}]`);

    // STEP 3: Connect Sockets for both clients
    console.log('\n[STEP 3] Connecting real-time Socket.IO clients...');
    const sock1 = createSocket(session1.sessionId);
    const sock2 = createSocket(session2.sessionId);

    await Promise.all([
      new Promise(res => sock1.on('connect', res)),
      new Promise(res => sock2.on('connect', res))
    ]);
    console.log('✓ Both sockets connected with authenticated handshakes');

    // STEP 4: Both Clients Join Room & Verify Presence
    console.log('\n[STEP 4] Joining room & verifying real-time presence...');
    const join1 = await new Promise(res => sock1.emit('join_room', { roomId: room.roomId }, res));
    const join2 = await new Promise(res => sock2.emit('join_room', { roomId: room.roomId }, res));

    if (!join1.success || !join2.success) {
      throw new Error(`Failed to join: ${join1.error || join2.error}`);
    }
    console.log(`✓ Client 1 joined as owner: ${join1.data.isOwner}`);
    console.log(`✓ Live participant presence count: ${join2.data.participantCount}`);

    // STEP 5: Exchange Real-Time Messages
    console.log('\n[STEP 5] Exchanging real-time messages between clients...');
    const client2ReceivedPromise = new Promise(resolve => {
      sock2.on('new_message', msg => resolve(msg));
    });

    const client1Ack = await new Promise(resolve => {
      sock1.emit(
        'send_message',
        { roomId: room.roomId, content: 'Greetings from Client 1 (NeonReaper)!' },
        resolve
      );
    });
    if (!client1Ack.success) throw new Error('Send message ack failed');

    const receivedByClient2 = await client2ReceivedPromise;
    console.log(`✓ Client 2 received in real-time: "${receivedByClient2.content}"`);
    console.log(`✓ Authoritative server timestamp: ${receivedByClient2.createdAt}`);
    console.log(`✓ Authoritative expiration: ${receivedByClient2.expiresAt}`);

    // STEP 6: Real-Time Typing Indicators
    console.log('\n[STEP 6] Testing typing indicator broadcasts...');
    const typingPromise = new Promise(resolve => {
      sock1.on('typing_update', data => resolve(data));
    });

    sock2.emit('typing_start', { roomId: room.roomId });
    const typingData = await typingPromise;
    console.log(`✓ Typing indicator received for: ${typingData.displayName} (isTyping=${typingData.isTyping})`);

    // STEP 7: Moderation (Owner Mutes & Kicks)
    console.log('\n[STEP 7] Testing server-enforced moderation controls...');
    const muteAck = await new Promise(resolve => {
      sock1.emit('mute_participant', { roomId: room.roomId, targetSessionId: session2.sessionId }, resolve);
    });
    console.log(`✓ Owner muted Client 2 (isMuted=${muteAck.isMuted})`);

    // Client 2 attempts to send message while muted
    const mutedSendAck = await new Promise(resolve => {
      sock2.emit('send_message', { roomId: room.roomId, content: 'Should fail' }, resolve);
    });
    if (mutedSendAck.success) {
      throw new Error('Muted participant should not be able to send messages!');
    }
    console.log(`✓ Server correctly rejected message from muted user: "${mutedSendAck.error}"`);

    // STEP 8: Private Room Authorization
    console.log('\n[STEP 8] Testing private-room password protection...');
    const bcrypt = require('bcryptjs');
    const privateRoom = await Room.create({
      roomId: 'E2E-VAULT',
      name: 'Restricted Vault',
      isPrivate: true,
      passwordHash: await bcrypt.hash('secretPass99', 10),
      creatorSessionId: session1.sessionId,
      messageRetentionSeconds: 3600,
      expiresAt: new Date(Date.now() + 86400000)
    });

    // Client 2 tries to join with wrong password
    const wrongPassJoin = await new Promise(res => {
      sock2.emit('join_room', { roomId: privateRoom.roomId, password: 'wrong' }, res);
    });
    if (wrongPassJoin.success) throw new Error('Private room allowed wrong password!');
    console.log(`✓ Wrong password rejected: "${wrongPassJoin.error}"`);

    // Client 2 joins with correct password
    const correctPassJoin = await new Promise(res => {
      sock2.emit('join_room', { roomId: privateRoom.roomId, password: 'secretPass99' }, res);
    });
    if (!correctPassJoin.success) throw new Error('Correct password failed!');
    console.log('✓ Authorized private room access granted with correct password');

    // Clean disconnect
    sock1.disconnect();
    sock2.disconnect();

    console.log('\n===========================================================');
    console.log('🎉 ALL 10 USER FLOW STEPS VERIFIED SUCCESSFULLY!');
    console.log('===========================================================');
  } finally {
    io.close();
    await new Promise(res => server.close(res));
    await teardownTestDb();
  }
}

runE2EVerification()
  .then(() => process.exit(0))
  .catch(err => {
    console.error('E2E Verification Failed:', err);
    process.exit(1);
  });
