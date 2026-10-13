const bcrypt = require('bcryptjs');
const Room = require('../models/Room');
const { generateRoomCode, validateDisplayName } = require('../utils/validation');
const { calculateRoomExpiration, getUnexpiredQueryFilter } = require('../utils/expiration');
const presenceService = require('./presenceService');

class RoomService {
  async createRoom({
    name,
    description = '',
    isPrivate = false,
    password = null,
    retentionSeconds = 3600,
    creatorSessionId
  }) {
    if (!name || name.trim().length < 2 || name.trim().length > 50) {
      const err = new Error('Room name must be between 2 and 50 characters.');
      err.status = 400;
      throw err;
    }

    if (!creatorSessionId) {
      const err = new Error('Creator session ID is required.');
      err.status = 400;
      throw err;
    }

    let passwordHash = null;
    if (isPrivate) {
      if (!password || password.length < 4) {
        const err = new Error('Private rooms require a password of at least 4 characters.');
        err.status = 400;
        throw err;
      }
      passwordHash = await bcrypt.hash(password, 10);
    }

    const validRetention = [1800, 3600, 7200].includes(Number(retentionSeconds))
      ? Number(retentionSeconds)
      : 3600;

    // Generate unique room code
    let roomId;
    let attempts = 0;
    while (!roomId && attempts < 10) {
      const candidate = generateRoomCode();
      const existing = await Room.findOne({ roomId: candidate });
      if (!existing) {
        roomId = candidate;
      }
      attempts++;
    }

    if (!roomId) {
      roomId = `DC${Date.now().toString().slice(-6)}`;
    }

    const expiresAt = calculateRoomExpiration(24);

    const room = await Room.create({
      roomId,
      name: name.trim(),
      description: (description || '').trim().slice(0, 200),
      isPrivate: Boolean(isPrivate),
      passwordHash,
      creatorSessionId,
      messageRetentionSeconds: validRetention,
      expiresAt
    });

    return {
      roomId: room.roomId,
      name: room.name,
      description: room.description,
      isPrivate: room.isPrivate,
      creatorSessionId: room.creatorSessionId,
      messageRetentionSeconds: room.messageRetentionSeconds,
      expiresAt: room.expiresAt,
      createdAt: room.createdAt,
      participantCount: 0
    };
  }

  async getPublicRooms({ search, page = 1, limit = 20 }) {
    const query = {
      isPrivate: false,
      ...getUnexpiredQueryFilter('expiresAt')
    };

    if (search && search.trim()) {
      query.name = { $regex: search.trim(), $options: 'i' };
    }

    const pageNum = Math.max(1, parseInt(page, 10) || 1);
    const limitNum = Math.min(50, Math.max(1, parseInt(limit, 10) || 20));
    const skip = (pageNum - 1) * limitNum;

    const [rooms, total] = await Promise.all([
      Room.find(query)
        .sort({ createdAt: -1 })
        .skip(skip)
        .limit(limitNum)
        .lean(),
      Room.countDocuments(query)
    ]);

    const roomsWithPresence = rooms.map(room => ({
      roomId: room.roomId,
      name: room.name,
      description: room.description,
      isPrivate: false,
      messageRetentionSeconds: room.messageRetentionSeconds,
      expiresAt: room.expiresAt,
      createdAt: room.createdAt,
      isClosed: room.isClosed,
      participantCount: presenceService.getParticipantCount(room.roomId)
    }));

    return {
      rooms: roomsWithPresence,
      pagination: {
        page: pageNum,
        limit: limitNum,
        total,
        totalPages: Math.ceil(total / limitNum)
      }
    };
  }

  async getRoomById(roomId) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    }).lean();

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    return {
      roomId: room.roomId,
      name: room.name,
      description: room.description,
      isPrivate: room.isPrivate,
      creatorSessionId: room.creatorSessionId,
      messageRetentionSeconds: room.messageRetentionSeconds,
      expiresAt: room.expiresAt,
      isClosed: room.isClosed,
      createdAt: room.createdAt,
      participantCount: presenceService.getParticipantCount(room.roomId)
    };
  }

  async verifyRoomAccess(roomId, { password, sessionId }) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    }).select('+passwordHash');

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    if (room.kickedSessionIds.includes(sessionId)) {
      const err = new Error('You have been kicked from this room and cannot rejoin.');
      err.status = 403;
      throw err;
    }

    if (room.isClosed && room.creatorSessionId !== sessionId && !presenceService.isSessionInRoom(roomId, sessionId)) {
      const err = new Error('This room is closed to new participants.');
      err.status = 403;
      throw err;
    }

    if (room.isPrivate) {
      // Room creator bypasses password prompt if session matches
      if (room.creatorSessionId !== sessionId) {
        if (!password) {
          const err = new Error('Password required for private room.');
          err.status = 401;
          err.requiresPassword = true;
          throw err;
        }

        const matches = await bcrypt.compare(password, room.passwordHash);
        if (!matches) {
          const err = new Error('Incorrect room password.');
          err.status = 401;
          throw err;
        }
      }
    }

    return room;
  }

  async closeRoom(roomId, sessionId) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    });

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    if (room.creatorSessionId !== sessionId) {
      const err = new Error('Only the room owner can close this room.');
      err.status = 403;
      throw err;
    }

    room.isClosed = true;
    await room.save();
    return room;
  }

  async deleteRoom(roomId, sessionId) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    });

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    if (room.creatorSessionId !== sessionId) {
      const err = new Error('Only the room owner can delete this room.');
      err.status = 403;
      throw err;
    }

    await Room.deleteOne({ roomId: room.roomId });
    presenceService.clearRoom(room.roomId);
    return true;
  }

  async kickParticipant(roomId, ownerSessionId, targetSessionId) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    });

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    if (room.creatorSessionId !== ownerSessionId) {
      const err = new Error('Only the room owner can remove participants.');
      err.status = 403;
      throw err;
    }

    if (room.creatorSessionId === targetSessionId) {
      const err = new Error('The room owner cannot be removed.');
      err.status = 400;
      throw err;
    }

    if (!room.kickedSessionIds.includes(targetSessionId)) {
      room.kickedSessionIds.push(targetSessionId);
      await room.save();
    }

    return true;
  }

  async muteParticipant(roomId, ownerSessionId, targetSessionId) {
    const room = await Room.findOne({
      roomId: roomId.toUpperCase(),
      ...getUnexpiredQueryFilter('expiresAt')
    });

    if (!room) {
      const err = new Error('Room not found or has expired.');
      err.status = 404;
      throw err;
    }

    if (room.creatorSessionId !== ownerSessionId) {
      const err = new Error('Only the room owner can mute participants.');
      err.status = 403;
      throw err;
    }

    if (room.creatorSessionId === targetSessionId) {
      const err = new Error('The room owner cannot be muted.');
      err.status = 400;
      throw err;
    }

    const isMuted = room.mutedSessionIds.includes(targetSessionId);
    if (!isMuted) {
      room.mutedSessionIds.push(targetSessionId);
    } else {
      // Toggle unmute if called again
      room.mutedSessionIds = room.mutedSessionIds.filter(id => id !== targetSessionId);
    }

    await room.save();
    return !isMuted; // returns true if now muted, false if unmuted
  }
}

module.exports = new RoomService();
