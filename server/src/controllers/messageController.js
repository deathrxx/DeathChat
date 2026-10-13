const messageService = require('../services/messageService');

async function getMessages(req, res, next) {
  try {
    const { roomId } = req.params;
    const { before, limit } = req.query;

    const messages = await messageService.getMessages(roomId, { before, limit });

    res.json({
      success: true,
      data: messages
    });
  } catch (err) {
    next(err);
  }
}

async function sendMessage(req, res, next) {
  try {
    const { roomId } = req.params;
    const { content } = req.body;

    const message = await messageService.createMessage({
      roomId,
      senderSession: req.session,
      content
    });

    res.status(201).json({
      success: true,
      data: message
    });
  } catch (err) {
    next(err);
  }
}

module.exports = {
  getMessages,
  sendMessage
};
