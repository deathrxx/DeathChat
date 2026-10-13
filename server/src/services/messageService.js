const { v4: uuidv4 } = require('uuid');
const Message = require('../models/Message');
const Room = require('../models/Room');
const { calculateMessageExpiration, getUnexpiredQueryFilter } = require('../utils/expiration');

class MessageService {
  async createMessage({ roomId, senderSession, content }) {
    if (!content || typeof content !== 'string' || content.trim().length === 0) {
      const err = new Error('Message content cannot be empty.');
      err.status = 400;
      throw err;
    }

    if (content.length > 1000) {
      const err = new Error('Message exceeds maximum length of 1000 characters.');
      err.status = 400;
      throw err;
    }

    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    });

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    if (room.kickedSessionIds.includes(senderSession.sessionId)) {
      const err = new Error('You have been kicked from this room.');
      err.status = 403;
      throw err;
    }

    if (room.mutedSessionIds.includes(senderSession.sessionId)) {
      const err = new Error('You are muted in this room.');
      err.status = 403;
      throw err;
    }

    const expiresAt = calculateMessageExpiration(room.messageRetentionSeconds, room.expiresAt);
    const messageId = uuidv4();

    const message = await Message.create({
      messageId,
      roomId: room.roomId,
      senderSessionId: senderSession.sessionId,
      senderDisplayName: senderSession.displayName,
      senderAvatar: senderSession.avatar,
      content: content.trim(),
      expiresAt
    });

    return {
      messageId: message.messageId,
      roomId: message.roomId,
      senderSessionId: message.senderSessionId,
      senderDisplayName: message.senderDisplayName,
      senderAvatar: message.senderAvatar,
      content: message.content,
      createdAt: message.createdAt,
      expiresAt: message.expiresAt
    };
  }

  async getMessages(roomId, { before = null, limit = 50 } = {}) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    });

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    const query = {
      roomId: room.roomId,
      ...getUnexpiredQueryFilter('expiresAt')
    };

    if (before) {
      const beforeDate = new Date(before);
      if (!isNaN(beforeDate.getTime())) {
        query.createdAt = { $lt: beforeDate };
      }
    }

    const limitNum = Math.min(100, Math.max(1, parseInt(limit, 10) || 50));

    const messages = await Message.find(query)
      .sort({ createdAt: -1 })
      .limit(limitNum)
      .lean();

    // Return in chronological order
    const ordered = messages.reverse().map(m => ({
      messageId: m.messageId,
      roomId: m.roomId,
      senderSessionId: m.senderSessionId,
      senderDisplayName: m.senderDisplayName,
      senderAvatar: m.senderAvatar,
      content: m.content,
      createdAt: m.createdAt,
      expiresAt: m.expiresAt
    }));

    return ordered;
  }
}

module.exports = new MessageService();
