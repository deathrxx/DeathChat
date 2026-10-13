const { v4: uuidv4 } = require('uuid');
const Report = require('../models/Report');

async function createReport(req, res, next) {
  try {
    const { reportedSessionId, roomId, reason } = req.body;

    if (!reportedSessionId || !reason || !reason.trim()) {
      return res.status(400).json({
        success: false,
        error: 'Reported session ID and a valid reason are required.'
      });
    }

    const report = await Report.create({
      reportId: uuidv4(),
      reporterSessionId: req.sessionId,
      reportedSessionId,
      roomId: (roomId || '').toUpperCase(),
      reason: reason.trim().slice(0, 500)
    });

    res.status(201).json({
      success: true,
      message: 'Report submitted successfully. Thank you for keeping the room safe.',
      data: {
        reportId: report.reportId
      }
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  createReport
};
