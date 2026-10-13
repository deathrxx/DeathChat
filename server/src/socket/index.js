const { Server } = require('socket.io');
const socketAuth = require('./socketAuth');
const { registerSocketHandlers } = require('./socketHandlers');

function initSocketServer(httpServer, corsOptions) {
  const io = new Server(httpServer, {
    cors: corsOptions,
    pingInterval: 25000,
    pingTimeout: 20000,
    transports: ['polling', 'websocket']
  });

  // Attach authentication middleware
  io.use(socketAuth);

  io.on('connection', socket => {
    console.log(`[SOCKET] Client connected: ${socket.id} (Session: ${socket.sessionId}, Codename: ${socket.session?.displayName}, Transport: ${socket.conn.transport.name})`);

    // Notify client of successful connection
    socket.emit('connection_status', {
      connected: true,
      sessionId: socket.sessionId,
      serverTime: new Date().toISOString()
    });

    // Register event listeners
    registerSocketHandlers(io, socket);

    socket.on('disconnect', reason => {
      console.log(`[SOCKET] Client disconnected: ${socket.id} (${socket.session?.displayName}), reason: ${reason}`);
    });
  });

  return io;
}

module.exports = {
  initSocketServer
};
