const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const config = require('./config');

// Route imports
const accountRoutes = require('./routes/accounts');
const transactionRoutes = require('./routes/transactions');
const settlementRoutes = require('./routes/settlement');
const hsmRoutes = require('./routes/hsm');

const app = express();

// Security middleware
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10kb' }));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000,
  max: 200
});
app.use('/api/', limiter);

// Routes
app.use('/api/accounts', accountRoutes);
app.use('/api/transactions', transactionRoutes);
app.use('/api/settlement', settlementRoutes);
app.use('/api/hsm', hsmRoutes);

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ 
    status: 'OK', 
    service: 'Mari Mock Bank',
    timestamp: new Date().toISOString()
  });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    success: false,
    error: 'Internal server error'
  });
});

// 404 handler
app.use((req, res) => {
  res.status(404).json({
    success: false,
    error: 'Endpoint not found'
  });
});

const PORT = config.port || 3001;

// Start server only if this file is run directly (not when imported by tests)
if (require.main === module) {
  app.listen(PORT, () => {
    console.log(`Mari Mock Bank server running on port ${PORT}`);
    console.log(`Bank: ${config.bank.name} (${config.bank.code})`);
  });
}

module.exports = app;
