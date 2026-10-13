const roomService = require('../services/roomService');
const presenceService = require('../services/presenceService');

async function createRoom(req, res, next) {
  try {
    const { name, description, isPrivate, password, retentionSeconds } = req.body;
    const room = await roomService.createRoom({
      name,
      description,
      isPrivate,
      password,
      retentionSeconds,
      creatorSessionId: req.sessionId
    });

    res.status(201).json({
      success: true,
      data: room
    });
  } catch (err) {
    next(err);
  }
}

async function getPublicRooms(req, res, next) {
  try {
    const { search, page, limit } = req.query;
    const result = await roomService.getPublicRooms({ search, page, limit });

    res.json({
      success: true,
      data: result.rooms,
      pagination: result.pagination
    });
  } catch (err) {
    next(err);
  }
}

async function getRoom(req, res, next) {
  try {
    const { roomId } = req.params;
    const room = await roomService.getRoomById(roomId);

    res.json({
      success: true,
      data: room
    });
  } catch (err) {
    next(err);
  }
}

async function joinRoom(req, res, next) {
  try {
    const { roomId } = req.params;
    const { password } = req.body;

    const room = await roomService.verifyRoomAccess(roomId, {
      password,
      sessionId: req.sessionId
    });

    res.json({
      success: true,
      data: {
        roomId: room.roomId,
        name: room.name,
        description: room.description,
        isPrivate: room.isPrivate,
        creatorSessionId: room.creatorSessionId,
        messageRetentionSeconds: room.messageRetentionSeconds,
        expiresAt: room.expiresAt,
        participantCount: presenceService.getParticipantCount(room.roomId)
      }
    });
  } catch (err) {
    next(err);
  }
}

async function getParticipants(req, res, next) {
  try {
    const { roomId } = req.params;
    const room = await roomService.getRoomById(roomId);
    const participants = presenceService.getParticipants(room.roomId, room.creatorSessionId);

    res.json({
      success: true,
      data: participants
    });
  } catch (err) {
    next(err);
  }
}

async function closeRoom(req, res, next) {
  try {
    const { roomId } = req.params;
    const room = await roomService.closeRoom(roomId, req.sessionId);

    res.json({
      success: true,
      message: 'Room has been closed to new participants.',
      data: {
        roomId: room.roomId,
        isClosed: room.isClosed
      }
    });
  } catch (err) {
    next(err);
  }
}

async function deleteRoom(req, res, next) {
  try {
    const { roomId } = req.params;
    await roomService.deleteRoom(roomId, req.sessionId);

    res.json({
      success: true,
      message: 'Room deleted successfully.'
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  createRoom,
  getPublicRooms,
  getRoom,
  joinRoom,
  getParticipants,
  closeRoom,
  deleteRoom
};
