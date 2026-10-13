const rateLimit = require('express-rate-limit');

// General API rate limiter
const apiLimiter = rateLimit({
  windowMs: parseInt(process.env.RATE_LIMIT_WINDOW_MS, 10) || 15 * 60 * 1000,
  max: parseInt(process.env.RATE_LIMIT_MAX_REQUESTS, 10) || 200,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: 'Too many requests. Please slow down.'
  }
});

// Stricter rate limiter for creating rooms (e.g. max 10 rooms per 15 minutes per IP)
const roomCreationLimiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 15,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: 'Too many rooms created recently. Please try again later.'
  }
});

// Stricter rate limiter for creating sessions
const sessionCreationLimiter = rateLimit({
  windowMs: 10 * 60 * 1000,
  max: 30,
  standardHeaders: true,
  legacyHeaders: false,
  message: {
    success: false,
    error: 'Too many session requests. Please try again later.'
  }
});

module.exports = {
  apiLimiter,
  roomCreationLimiter,
  sessionCreationLimiter
};
