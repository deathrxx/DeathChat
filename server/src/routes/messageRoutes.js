const express = require('express');
const router = express.Router({ mergeParams: true });
const messageController = require('../controllers/messageController');
const { authenticateSession } = require('../middleware/authenticateSession');

router.get('/:roomId/messages', messageController.getMessages);
router.post('/:roomId/messages', authenticateSession, messageController.sendMessage);

module.exports = router;
