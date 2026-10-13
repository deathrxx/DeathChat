const mongoose = require('mongoose');

const sessionSchema = new mongoose.Schema(
  {
    sessionId: {
      type: String,
      required: true,
      unique: true,
      index: true
    },
    displayName: {
      type: String,
      required: true,
      trim: true,
      minlength: 2,
      maxlength: 32
    },
    avatar: {
      type: String,
      required: true,
      default: 'avatar_reaper'
    },
    lastActiveAt: {
      type: Date,
      default: Date.now
    }
  },
  {
    timestamps: true
  }
);

// Expire inactive anonymous sessions after 7 days
sessionSchema.index({ lastActiveAt: 1 }, { expireAfterSeconds: 7 * 24 * 3600 });

module.exports = mongoose.model('Session', sessionSchema);
