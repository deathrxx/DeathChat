const mongoose = require('mongoose');

const roomSchema = new mongoose.Schema(
  {
    roomId: {
      type: String,
      required: true,
      unique: true,
      uppercase: true,
      trim: true,
      index: true
    },
    name: {
      type: String,
      required: true,
      trim: true,
      minlength: 2,
      maxlength: 50
    },
    description: {
      type: String,
      trim: true,
      maxlength: 200,
      default: ''
    },
    isPrivate: {
      type: Boolean,
      default: false,
      index: true
    },
    passwordHash: {
      type: String,
      select: false, // Never returned in queries unless explicitly requested with .select('+passwordHash')
      default: null
    },
    creatorSessionId: {
      type: String,
      required: true,
      index: true
    },
    messageRetentionSeconds: {
      type: Number,
      required: true,
      enum: [1800, 3600, 7200], // 30 mins, 1 hour, 2 hours
      default: 3600
    },
    expiresAt: {
      type: Date,
      required: true
    },
    isClosed: {
      type: Boolean,
      default: false
    },
    mutedSessionIds: {
      type: [String],
      default: []
    },
    kickedSessionIds: {
      type: [String],
      default: []
    }
  },
  {
    timestamps: true
  }
);

// TTL index to automatically remove physically expired documents in MongoDB
roomSchema.index({ expiresAt: 1 }, { expireAfterSeconds: 0 });

// Helper to check if room is currently valid and unexpired
roomSchema.methods.isExpired = function () {
  return new Date() >= this.expiresAt;
};

module.exports = mongoose.model('Room', roomSchema);
