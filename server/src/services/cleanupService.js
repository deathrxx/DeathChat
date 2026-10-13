const Room = require('../models/Room');
const Message = require('../models/Message');
const presenceService = require('./presenceService');

class CleanupService {
  constructor() {
    this.intervalId = null;
    this.io = null;
  }

  setSocketServer(io) {
    this.io = io;
  }

  start(intervalMs = 60000) {
    if (this.intervalId) return;

    this.intervalId = setInterval(() => {
      this.runCleanup().catch(err => {
        console.error('[CLEANUP] Error during periodic cleanup:', err);
      });
    }, intervalMs);

    console.log(`[CLEANUP] Periodic expiration service started (${intervalMs}ms interval).`);
  }

  stop() {
    if (this.intervalId) {
      clearInterval(this.intervalId);
      this.intervalId = null;
      console.log('[CLEANUP] Periodic expiration service stopped.');
    }
  }

  async runCleanup() {
    const now = new Date();

    // 1. Delete physically expired messages
    const messageRes = await Message.deleteMany({ expiresAt: { $lte: now } });
    if (messageRes.deletedCount > 0) {
      console.log(`[CLEANUP] Pruned ${messageRes.deletedCount} expired messages.`);
    }

    // 2. Identify expired rooms
    const expiredRooms = await Room.find({ expiresAt: { $lte: now } });
    for (const room of expiredRooms) {
      console.log(`[CLEANUP] Room ${room.roomId} has expired. Evicting presence...`);

      if (this.io) {
        this.io.to(room.roomId).emit('room_expired', {
          roomId: room.roomId,
          message: 'This room has reached its 24-hour expiration limit and is now closed.'
        });
      }

      presenceService.clearRoom(room.roomId);
    }

    if (expiredRooms.length > 0) {
      const roomRes = await Room.deleteMany({ expiresAt: { $lte: now } });
      console.log(`[CLEANUP] Pruned ${roomRes.deletedCount} expired rooms.`);
    }
  }
}

module.exports = new CleanupService();
