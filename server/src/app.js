const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const path = require('path');

const sessionRoutes = require('./routes/sessionRoutes');
const roomRoutes = require('./routes/roomRoutes');
const messageRoutes = require('./routes/messageRoutes');
const reportRoutes = require('./routes/reportRoutes');
const errorHandler = require('./middleware/errorHandler');
const { apiLimiter } = require('./middleware/rateLimiter');

function createApp() {
  const app = express();

  // Helmet with relaxed content security policy for local dev & websockets
  app.use(
    helmet({
      contentSecurityPolicy: false,
      crossOriginEmbedderPolicy: false
    })
  );

  // CORS configuration
  const allowedOrigins = process.env.ALLOWED_ORIGINS
    ? process.env.ALLOWED_ORIGINS.split(',').map(s => s.trim())
    : ['*'];

  const corsOptions = {
    origin: (origin, callback) => {
      if (!origin || allowedOrigins.includes('*') || allowedOrigins.includes(origin)) {
        callback(null, true);
      } else {
        callback(null, true); // Allow all in dev mode for mobile emulator / testing
      }
    },
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'x-session-id'],
    credentials: true
  };

  app.use(cors(corsOptions));
  app.use(express.json({ limit: '1mb' }));
  app.use(express.urlencoded({ extended: true, limit: '1mb' }));

  // Health check endpoint
  app.get(['/health', '/api/health'], (req, res) => {
    res.json({
      status: 'ok',
      service: 'DeathChat API',
      version: '1.0.0',
      uptime: Math.floor(process.uptime()),
      timestamp: new Date().toISOString()
    });
  });

  // Apply general API rate limiter
  app.use('/api', apiLimiter);

  // Mount API routes
  app.use('/api/sessions', sessionRoutes);
  app.use('/api/rooms', roomRoutes);
  app.use('/api/rooms', messageRoutes);
  app.use('/api/reports', reportRoutes);

  // Serve companion web client if available
  const webPath = path.join(__dirname, '../../web');
  app.use(express.static(webPath));

  // Centralized Error Handler
  app.use(errorHandler);

  return { app, corsOptions };
}

module.exports = createApp;
