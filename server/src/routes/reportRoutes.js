const express = require('express');
const router = express.Router();
const reportController = require('../controllers/reportController');
const { authenticateSession } = require('../middleware/authenticateSession');

router.post('/', authenticateSession, reportController.createReport);

module.exports = router;
