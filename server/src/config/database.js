const mongoose = require('mongoose');

let mongoMemoryServer = null;

async function connectDatabase() {
  const customUri = process.env.MONGODB_URI;

  if (customUri && customUri.trim() !== '') {
    try {
      console.log(`[DB] Connecting to MongoDB at ${customUri}...`);
      await mongoose.connect(customUri);
      console.log('[DB] Connected to MongoDB successfully.');
      return mongoose.connection;
    } catch (err) {
      console.warn(`[DB] Failed to connect to ${customUri}: ${err.message}`);
      if (process.env.NODE_ENV === 'production') {
        throw err;
      }
      console.log('[DB] Falling back to embedded MongoMemoryServer for development/testing...');
    }
  }

  // Fallback to in-memory MongoDB for seamless zero-config local development and testing
  try {
    const { MongoMemoryServer } = require('mongodb-memory-server');
    mongoMemoryServer = await MongoMemoryServer.create();
    const uri = mongoMemoryServer.getUri();
    console.log(`[DB] Starting embedded MongoMemoryServer at ${uri}...`);
    await mongoose.connect(uri);
    console.log('[DB] Connected to embedded MongoMemoryServer successfully.');
    return mongoose.connection;
  } catch (err) {
    console.error('[DB] Failed to initialize MongoMemoryServer:', err);
    throw err;
  }
}

async function disconnectDatabase() {
  try {
    await mongoose.disconnect();
    if (mongoMemoryServer) {
      await mongoMemoryServer.stop();
      mongoMemoryServer = null;
    }
    console.log('[DB] Database disconnected.');
  } catch (err) {
    console.error('[DB] Error during disconnect:', err);
  }
}

module.exports = {
  connectDatabase,
  disconnectDatabase
};
