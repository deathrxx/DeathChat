const roomService = require('../services/roomService');
const messageService = require('../services/messageService');
const presenceService = require('../services/presenceService');

// Per-socket message rate limit tracker
const socketMessageCounts = new Map();

function isMessageRateLimited(socketId) {
  const now = Date.now();
  const record = socketMessageCounts.get(socketId) || { count: 0, resetAt: now + 5000 };

  if (now > record.resetAt) {
    record.count = 1;
    record.resetAt = now + 5000;
  } else {
    record.count++;
  }

  socketMessageCounts.set(socketId, record);
  return record.count > 10; // Max 10 messages per 5 seconds
}

function registerSocketHandlers(io, socket) {
  const sessionId = socket.sessionId;
  const session = socket.session;

  // Typing timeout cleanup
  let typingTimeout = null;

  // 1. JOIN ROOM
  socket.on('join_room', async (data = {}, callback) => {
    try {
      const { roomId, password } = data;
      if (!roomId) {
        const errPayload = { success: false, error: 'Room ID is required.' };
        if (typeof callback === 'function') callback(errPayload);
        socket.emit('room_error', errPayload);
        return;
      }

      const room = await roomService.verifyRoomAccess(roomId, {
        password,
        sessionId
      });

      const normalizedRoomId = room.roomId;
      socket.join(normalizedRoomId);

      // Add to presence
      presenceService.addParticipant(normalizedRoomId, session, socket.id);
      const participants = presenceService.getParticipants(normalizedRoomId, room.creatorSessionId);
      const isOwner = room.creatorSessionId === sessionId;

      const joinPayload = {
        success: true,
        data: {
          roomId: room.roomId,
          name: room.name,
          description: room.description,
          isPrivate: room.isPrivate,
          isOwner,
          messageRetentionSeconds: room.messageRetentionSeconds,
          expiresAt: room.expiresAt,
          participantCount: participants.length,
          participants
        }
      };

      if (typeof callback === 'function') {
        callback(joinPayload);
      }
      socket.emit('room_joined', joinPayload);

      // Notify others in room
      socket.to(normalizedRoomId).emit('participant_joined', {
        sessionId: session.sessionId,
        displayName: session.displayName,
        avatar: session.avatar,
        isOwner
      });

      io.to(normalizedRoomId).emit('participants_updated', {
        count: participants.length,
        participants
      });
    } catch (err) {
      const errPayload = {
        success: false,
        error: err.message || 'Failed to join room.',
        requiresPassword: !!err.requiresPassword
      };
      if (typeof callback === 'function') {
        callback(errPayload);
      }
      socket.emit('room_error', errPayload);
    }
  });

  // 2. SEND MESSAGE
  socket.on('send_message', async (data = {}, callback) => {
    try {
      if (isMessageRateLimited(socket.id)) {
        const rateLimitErr = { success: false, error: 'Sending messages too fast. Please slow down.' };
        if (typeof callback === 'function') callback(rateLimitErr);
        socket.emit('message_rejected', rateLimitErr);
        return;
      }

      const { roomId, content } = data;
      if (!roomId || !content) {
        const badPayload = { success: false, error: 'Room ID and message content are required.' };
        if (typeof callback === 'function') callback(badPayload);
        socket.emit('message_rejected', badPayload);
        return;
      }

      const message = await messageService.createMessage({
        roomId,
        senderSession: session,
        content
      });

      // Clear typing indicator
      socket.to(message.roomId).emit('typing_update', {
        sessionId,
        displayName: session.displayName,
        isTyping: false
      });

      // Broadcast new message to all in room
      io.to(message.roomId).emit('new_message', message);

      if (typeof callback === 'function') {
        callback({ success: true, data: message });
      }
    } catch (err) {
      const errPayload = {
        success: false,
        error: err.message || 'Failed to send message.'
      };
      if (typeof callback === 'function') {
        callback(errPayload);
      }
      socket.emit('message_rejected', errPayload);
    }
  });

  // 3. TYPING INDICATORS
  socket.on('typing_start', (data = {}) => {
    const { roomId } = data;
    if (!roomId) return;
    const norm = roomId.toUpperCase();

    if (typingTimeout) clearTimeout(typingTimeout);

    socket.to(norm).emit('typing_update', {
      sessionId,
      displayName: session.displayName,
      isTyping: true
    });

    // Auto-stop typing after 5 seconds if client disconnected or stalled
    typingTimeout = setTimeout(() => {
      socket.to(norm).emit('typing_update', {
        sessionId,
        displayName: session.displayName,
        isTyping: false
      });
    }, 5000);
  });

  socket.on('typing_stop', (data = {}) => {
    const { roomId } = data;
    if (!roomId) return;
    const norm = roomId.toUpperCase();

    if (typingTimeout) clearTimeout(typingTimeout);

    socket.to(norm).emit('typing_update', {
      sessionId,
      displayName: session.displayName,
      isTyping: false
    });
  });

  // 4. KICK PARTICIPANT (OWNER ONLY)
  socket.on('kick_participant', async (data = {}, callback) => {
    try {
      const { roomId, targetSessionId } = data;
      if (!roomId || !targetSessionId) {
        const payload = { success: false, error: 'Room ID and target session ID are required.' };
        if (typeof callback === 'function') callback(payload);
        return;
      }

      await roomService.kickParticipant(roomId, sessionId, targetSessionId);

      const norm = roomId.toUpperCase();
      // Remove all sockets of target session
      const targetSockets = presenceService.removeSessionCompletely(norm, targetSessionId);

      for (const sid of targetSockets) {
        const targetSock = io.sockets.sockets.get(sid);
        if (targetSock) {
          targetSock.emit('participant_kicked', {
            roomId: norm,
            message: 'You have been removed from this room by the owner.'
          });
          targetSock.leave(norm);
        }
      }

      const participants = presenceService.getParticipants(norm, sessionId);
      io.to(norm).emit('participant_left', {
        sessionId: targetSessionId,
        reason: 'kicked'
      });
      io.to(norm).emit('participants_updated', {
        count: participants.length,
        participants
      });

      if (typeof callback === 'function') {
        callback({ success: true, message: 'Participant kicked successfully.' });
      }
    } catch (err) {
      if (typeof callback === 'function') {
        callback({ success: false, error: err.message });
      }
    }
  });

  // 5. MUTE PARTICIPANT (OWNER ONLY)
  socket.on('mute_participant', async (data = {}, callback) => {
    try {
      const { roomId, targetSessionId } = data;
      if (!roomId || !targetSessionId) {
        const payload = { success: false, error: 'Room ID and target session ID are required.' };
        if (typeof callback === 'function') callback(payload);
        return;
      }

      const isMuted = await roomService.muteParticipant(roomId, sessionId, targetSessionId);
      const norm = roomId.toUpperCase();

      io.to(norm).emit('participant_muted', {
        sessionId: targetSessionId,
        isMuted
      });

      if (typeof callback === 'function') {
        callback({ success: true, isMuted });
      }
    } catch (err) {
      if (typeof callback === 'function') {
        callback({ success: false, error: err.message });
      }
    }
  });

  // 6. CLOSE ROOM (OWNER ONLY)
  socket.on('close_room', async (data = {}, callback) => {
    try {
      const { roomId } = data;
      await roomService.closeRoom(roomId, sessionId);
      const norm = roomId.toUpperCase();

      io.to(norm).emit('room_closed', {
        roomId: norm,
        message: 'This room has been closed to new participants.'
      });

      if (typeof callback === 'function') {
        callback({ success: true, message: 'Room closed.' });
      }
    } catch (err) {
      if (typeof callback === 'function') {
        callback({ success: false, error: err.message });
      }
    }
  });

  // 7. DELETE ROOM (OWNER ONLY)
  socket.on('delete_room', async (data = {}, callback) => {
    try {
      const { roomId } = data;
      await roomService.deleteRoom(roomId, sessionId);
      const norm = roomId.toUpperCase();

      io.to(norm).emit('room_deleted', {
        roomId: norm,
        message: 'The room has been deleted by the owner.'
      });

      // Remove all sockets from this room
      io.in(norm).socketsLeave(norm);

      if (typeof callback === 'function') {
        callback({ success: true, message: 'Room deleted.' });
      }
    } catch (err) {
      if (typeof callback === 'function') {
        callback({ success: false, error: err.message });
      }
    }
  });

  // 8. LEAVE ROOM
  socket.on('leave_room', (data = {}, callback) => {
    const { roomId } = data;
    if (!roomId) return;
    const norm = roomId.toUpperCase();

    socket.leave(norm);
    const result = presenceService.removeParticipant(socket.id);

    if (result && result.sessionCompletelyLeft) {
      socket.to(norm).emit('participant_left', {
        sessionId,
        displayName: session.displayName
      });

      const participants = presenceService.getParticipants(norm);
      io.to(norm).emit('participants_updated', {
        count: participants.length,
        participants
      });
    }

    if (typeof callback === 'function') {
      callback({ success: true });
    }
  });

  // 9. DISCONNECT
  socket.on('disconnect', () => {
    if (typingTimeout) clearTimeout(typingTimeout);
    socketMessageCounts.delete(socket.id);

    const result = presenceService.removeParticipant(socket.id);
    if (result && result.sessionCompletelyLeft) {
      socket.to(result.roomId).emit('participant_left', {
        sessionId,
        displayName: session.displayName
      });

      const participants = presenceService.getParticipants(result.roomId);
      io.to(result.roomId).emit('participants_updated', {
        count: result.remainingCount,
        participants
      });
    }
  });
}

module.exports = {
  registerSocketHandlers
};
