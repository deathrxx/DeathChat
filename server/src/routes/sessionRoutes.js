const express = require('express');
const router = express.Router();
const sessionController = require('../controllers/sessionController');
const { authenticateSession } = require('../middleware/authenticateSession');
const { sessionCreationLimiter } = require('../middleware/rateLimiter');

router.post('/', sessionCreationLimiter, sessionController.createSession);
router.get('/random-identity', sessionController.getRandomIdentity);
router.get('/me', authenticateSession, sessionController.getSession);
router.put('/me', authenticateSession, sessionController.updateSession);

module.exports = router;
