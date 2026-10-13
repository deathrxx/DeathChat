const Session = require('../models/Session');

async function authenticateSession(req, res, next) {
  try {
    let sessionId = req.headers['x-session-id'];

    if (!sessionId && req.headers.authorization) {
      const parts = req.headers.authorization.split(' ');
      if (parts.length === 2 && (parts[0] === 'Bearer' || parts[0] === 'Session')) {
        sessionId = parts[1];
      }
    }

    if (!sessionId) {
      return res.status(401).json({
        success: false,
        error: 'Authentication required. No session ID provided.'
      });
    }

    const session = await Session.findOne({ sessionId });
    if (!session) {
      return res.status(401).json({
        success: false,
        error: 'Invalid or expired session. Please generate a new identity.'
      });
    }

    // Touch last active timestamp
    session.lastActiveAt = new Date();
    await session.save();

    req.session = session;
    req.sessionId = session.sessionId;
    next();
  } catch (err) {
    next(err);
  }
}

async function optionalSession(req, res, next) {
  try {
    let sessionId = req.headers['x-session-id'];
    if (!sessionId && req.headers.authorization) {
      const parts = req.headers.authorization.split(' ');
      if (parts.length === 2) {
        sessionId = parts[1];
      }
    }

    if (sessionId) {
      const session = await Session.findOne({ sessionId });
      if (session) {
        session.lastActiveAt = new Date();
        await session.save();
        req.session = session;
        req.sessionId = session.sessionId;
      }
    }
    next();
  } catch (err) {
    next(err);
  }
}

module.exports = {
  authenticateSession,
  optionalSession
};
