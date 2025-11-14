// server.js
const app = require('./src/app');
const config = require('./src/config');
const mongoose = require('mongoose');

const startServer = async () => {
  try {
    // Connect to MongoDB
    await mongoose.connect(config.database.url);
    console.log('Connected to MongoDB');

    // Start server
    const server = app.listen(config.port, () => {
      console.log(`Mari Core Server running on port ${config.port}`);
    });

    // Graceful shutdown (Mongoose v7: close() returns a promise; callbacks no longer supported)
    const shutdown = async (signal) => {
      try {
        console.log(`${signal} received, shutting down gracefully`);
        await new Promise((resolve) => server.close(resolve));
        await mongoose.connection.close(false);
        console.log('Server closed');
        process.exit(0);
      } catch (err) {
        console.error('Error during shutdown:', err);
        process.exit(1);
      }
    };

    process.on('SIGTERM', () => { shutdown('SIGTERM'); });
    process.on('SIGINT', () => { shutdown('SIGINT'); });

  } catch (error) {
    console.error('Failed to start server:', error);
    process.exit(1);
  }
};

startServer();
