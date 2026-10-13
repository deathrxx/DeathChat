/**
 * PresenceService
 * Tracks real-time active room participants and their socket connections in memory.
 * Prevents phantom participant counts when users refresh, open multiple tabs, or reconnect.
 */

class PresenceService {
  constructor() {
    // roomId -> Map(sessionId -> { displayName, avatar, socketIds: Set, joinedAt })
    this.rooms = new Map();
    // socketId -> { roomId, sessionId }
    this.socketMap = new Map();
  }

  addParticipant(roomId, session, socketId) {
    if (!this.rooms.has(roomId)) {
      this.rooms.set(roomId, new Map());
    }

    const roomParticipants = this.rooms.get(roomId);
    const sessionId = session.sessionId;

    if (!roomParticipants.has(sessionId)) {
      roomParticipants.set(sessionId, {
        sessionId: session.sessionId,
        displayName: session.displayName,
        avatar: session.avatar,
        socketIds: new Set([socketId]),
        joinedAt: new Date()
      });
    } else {
      const existing = roomParticipants.get(sessionId);
      existing.socketIds.add(socketId);
      // Update display name or avatar if updated
      existing.displayName = session.displayName;
      existing.avatar = session.avatar;
    }

    this.socketMap.set(socketId, { roomId, sessionId });
    return this.getParticipants(roomId);
  }

  removeParticipant(socketId) {
    const meta = this.socketMap.get(socketId);
    if (!meta) return null;

    const { roomId, sessionId } = meta;
    this.socketMap.delete(socketId);

    const roomParticipants = this.rooms.get(roomId);
    if (!roomParticipants) return null;

    const participant = roomParticipants.get(sessionId);
    if (!participant) return null;

    participant.socketIds.delete(socketId);

    let sessionCompletelyLeft = false;
    if (participant.socketIds.size === 0) {
      roomParticipants.delete(sessionId);
      sessionCompletelyLeft = true;
    }

    if (roomParticipants.size === 0) {
      this.rooms.delete(roomId);
    }

    return {
      roomId,
      sessionId,
      participant,
      sessionCompletelyLeft,
      remainingCount: this.getParticipantCount(roomId)
    };
  }

  removeSessionCompletely(roomId, sessionId) {
    const roomParticipants = this.rooms.get(roomId);
    if (!roomParticipants) return [];

    const participant = roomParticipants.get(sessionId);
    if (!participant) return [];

    const socketIds = Array.from(participant.socketIds);
    for (const sid of socketIds) {
      this.socketMap.delete(sid);
    }

    roomParticipants.delete(sessionId);
    if (roomParticipants.size === 0) {
      this.rooms.delete(roomId);
    }

    return socketIds;
  }

  getParticipants(roomId, creatorSessionId = null) {
    const roomParticipants = this.rooms.get(roomId);
    if (!roomParticipants) return [];

    return Array.from(roomParticipants.values()).map(p => ({
      sessionId: p.sessionId,
      displayName: p.displayName,
      avatar: p.avatar,
      isOwner: creatorSessionId ? p.sessionId === creatorSessionId : false,
      joinedAt: p.joinedAt
    }));
  }

  getParticipantCount(roomId) {
    const roomParticipants = this.rooms.get(roomId);
    return roomParticipants ? roomParticipants.size : 0;
  }

  isSessionInRoom(roomId, sessionId) {
    const roomParticipants = this.rooms.get(roomId);
    return !!(roomParticipants && roomParticipants.has(sessionId));
  }

  clearRoom(roomId) {
    const roomParticipants = this.rooms.get(roomId);
    if (!roomParticipants) return;

    for (const p of roomParticipants.values()) {
      for (const sid of p.socketIds) {
        this.socketMap.delete(sid);
      }
    }
    this.rooms.delete(roomId);
  }
}

module.exports = new PresenceService();
