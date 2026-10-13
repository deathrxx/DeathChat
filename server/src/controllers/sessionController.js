const { v4: uuidv4 } = require('uuid');
const Session = require('../models/Session');
const {
  generateRandomDisplayName,
  getRandomAvatar,
  validateDisplayName,
  AVATARS
} = require('../utils/validation');

async function createSession(req, res, next) {
  try {
    let { displayName, avatar } = req.body;

    if (!displayName || typeof displayName !== 'string' || !displayName.trim()) {
      displayName = generateRandomDisplayName();
    } else {
      displayName = displayName.trim();
      if (!validateDisplayName(displayName)) {
        return res.status(400).json({
          success: false,
          error: 'Display name must be between 2 and 32 characters (letters, numbers, dashes, underscores).'
        });
      }
    }

    if (!avatar || !AVATARS.includes(avatar)) {
      avatar = getRandomAvatar();
    }

    const sessionId = uuidv4();

    const session = await Session.create({
      sessionId,
      displayName,
      avatar,
      lastActiveAt: new Date()
    });

    res.status(201).json({
      success: true,
      data: {
        sessionId: session.sessionId,
        displayName: session.displayName,
        avatar: session.avatar,
        createdAt: session.createdAt
      }
    });
  } catch (err) {
    next(err);
  }
}

async function getSession(req, res, next) {
  try {
    res.json({
      success: true,
      data: {
        sessionId: req.session.sessionId,
        displayName: req.session.displayName,
        avatar: req.session.avatar,
        lastActiveAt: req.session.lastActiveAt
      }
    });
  } catch (err) {
    next(err);
  }
}

async function updateSession(req, res, next) {
  try {
    const { displayName, avatar } = req.body;

    if (displayName !== undefined) {
      const trimmed = displayName.trim();
      if (!validateDisplayName(trimmed)) {
        return res.status(400).json({
          success: false,
          error: 'Display name must be between 2 and 32 characters (letters, numbers, dashes, underscores).'
        });
      }
      req.session.displayName = trimmed;
    }

    if (avatar !== undefined) {
      if (AVATARS.includes(avatar)) {
        req.session.avatar = avatar;
      }
    }

    req.session.lastActiveAt = new Date();
    await req.session.save();

    res.json({
      success: true,
      data: {
        sessionId: req.session.sessionId,
        displayName: req.session.displayName,
        avatar: req.session.avatar
      }
    });
  } catch (err) {
    next(err);
  }
}

async function getRandomIdentity(req, res, next) {
  try {
    res.json({
      success: true,
      data: {
        displayName: generateRandomDisplayName(),
        avatar: getRandomAvatar(),
        availableAvatars: AVATARS
      }
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  createSession,
  getSession,
  updateSession,
  getRandomIdentity
};
