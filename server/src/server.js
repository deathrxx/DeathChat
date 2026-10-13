require('dotenv').config();
const http = require('http');
const createApp = require('./app');
const { connectDatabase, disconnectDatabase } = require('./config/database');
const { initSocketServer } = require('./socket');
const cleanupService = require('./services/cleanupService');

const PORT = parseInt(process.env.PORT, 10) || 3000;

async function startServer() {
  try {
    // 1. Connect to Database
    await connectDatabase();

    // 2. Initialize App and HTTP server
    const { app, corsOptions } = createApp();
    const server = http.createServer(app);

    // 3. Initialize Socket.IO
    const io = initSocketServer(server, corsOptions);

    // 4. Start periodic cleanup service (every 60 seconds)
    cleanupService.setSocketServer(io);
    cleanupService.start(60000);

    // 5. Start listening
    server.listen(PORT, '0.0.0.0', () => {
      console.log('====================================================');
      console.log(`💀 DEATHCHAT BACKEND RUNNING ON PORT: ${PORT}`);
      console.log(`🔗 REST API:   http://localhost:${PORT}/api`);
      console.log(`🏥 HEALTH:     http://localhost:${PORT}/health`);
      console.log(`🌐 COMPANION:  http://localhost:${PORT}/`);
      console.log('====================================================');
    });

    // Graceful shutdown handling
    const shutdown = async signal => {
      console.log(`\n[SHUTDOWN] Received ${signal}. Gracefully stopping...`);
      cleanupService.stop();
      io.close(() => {
        console.log('[SOCKET] Socket server closed.');
      });
      server.close(async () => {
        console.log('[HTTP] HTTP server closed.');
        await disconnectDatabase();
        process.exit(0);
      });
    };

    process.on('SIGINT', () => shutdown('SIGINT'));
    process.on('SIGTERM', () => shutdown('SIGTERM'));

    return { server, io };
  } catch (err) {
    console.error('[FATAL] Failed to start server:', err);
    process.exit(1);
  }
}

// Only start immediately if executed directly (not when required in tests)
if (require.main === module) {
  startServer();
}

module.exports = { startServer };
