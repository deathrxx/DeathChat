const Session = require('../models/Session');

async function socketAuth(socket, next) {
  try {
    const auth = socket.handshake.auth || {};
    const query = socket.handshake.query || {};

    const sessionId = auth.token || auth.sessionId || query.sessionId || query.token;

    if (!sessionId) {
      console.warn(`[SOCKET AUTH] Rejected handshake from ${socket.id} (address: ${socket.handshake.address}): Missing session ID.`);
      return next(new Error('Authentication failed: Missing session ID.'));
    }

    const session = await Session.findOne({ sessionId });
    if (!session) {
      console.warn(`[SOCKET AUTH] Rejected handshake from ${socket.id}: Session '${sessionId}' not found or expired.`);
      return next(new Error('Authentication failed: Invalid or expired session.'));
    }

    // Touch last active
    session.lastActiveAt = new Date();
    await session.save();

    socket.session = session;
    socket.sessionId = session.sessionId;
    next();
  } catch (err) {
    console.error('[SOCKET AUTH ERROR]', err);
    next(new Error('Internal server error during socket authentication.'));
  }
}

module.exports = socketAuth;
