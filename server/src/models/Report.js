const mongoose = require('mongoose');

const reportSchema = new mongoose.Schema(
  {
    reportId: {
      type: String,
      required: true,
      unique: true,
      index: true
    },
    reporterSessionId: {
      type: String,
      required: true,
      index: true
    },
    reportedSessionId: {
      type: String,
      required: true,
      index: true
    },
    roomId: {
      type: String,
      required: true
    },
    reason: {
      type: String,
      required: true,
      trim: true,
      minlength: 2,
      maxlength: 500
    }
  },
  {
    timestamps: true
  }
);

module.exports = mongoose.model('Report', reportSchema);
