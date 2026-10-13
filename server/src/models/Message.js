const mongoose = require('mongoose');

const messageSchema = new mongoose.Schema(
  {
    messageId: {
      type: String,
      required: true,
      unique: true,
      index: true
    },
    roomId: {
      type: String,
      required: true,
      index: true
    },
    senderSessionId: {
      type: String,
      required: true
    },
    senderDisplayName: {
      type: String,
      required: true,
      trim: true
    },
    senderAvatar: {
      type: String,
      required: true
    },
    content: {
      type: String,
      required: true,
      trim: true,
      minlength: 1,
      maxlength: 1000
    },
    expiresAt: {
      type: Date,
      required: true
    }
  },
  {
    timestamps: true
  }
);

// TTL index to automatically remove physically expired documents in MongoDB
messageSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

module.exports = mongoose.model('Message', messageSchema);
