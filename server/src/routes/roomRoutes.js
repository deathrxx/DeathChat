const express = require('express');
const router = express.Router();
const roomController = require('../controllers/roomController');
const { authenticateSession } = require('../middleware/authenticateSession');
const { roomCreationLimiter } = require('../middleware/rateLimiter');

// Public room discovery (does not require auth)
router.get('/', roomController.getPublicRooms);

// Authenticated room operations
router.post('/', authenticateSession, roomCreationLimiter, roomController.createRoom);
router.get('/:roomId', roomController.getRoom);
router.post('/:roomId/join', authenticateSession, roomController.joinRoom);
router.get('/:roomId/participants', roomController.getParticipants);
router.post('/:roomId/close', authenticateSession, roomController.closeRoom);
router.delete('/:roomId', authenticateSession, roomController.deleteRoom);

module.exports = router;
