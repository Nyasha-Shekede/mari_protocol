// src/app.js
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { errorHandler } = require('./middleware/errorHandler');

// Route imports
const transactionRoutes = require('./routes/transactions');
const userRoutes = require('./routes/users');
const authRoutes = require('./routes/auth');
const merchantRoutes = require('./routes/merchant');
const batchRoutes = require('./routes/batch');
const settlementRoutes = require('./routes/settlement');

const app = express();
const axios = require('axios');

// Security middleware
app.use(helmet());
app.use(cors());
app.use(express.json({ limit: '10kb' }));
// Support form-encoded webhooks (e.g., Twilio)
app.use(express.urlencoded({ extended: false }));

// Rate limiting
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: 100 // limit each IP to 100 requests per windowMs
});
app.use('/api/', limiter);
// Apply rate limiting to SMS webhooks as well
app.use('/webhook/sms', limiter);

// Physics validation removed per directive (Quantum Seal now in coupon; server validates signatures & idempotency)

// Routes
app.use('/api/auth', authRoutes);
app.use('/api/transactions', transactionRoutes);
app.use('/api/batch', batchRoutes);
app.use('/api/settlement', settlementRoutes);
// Penalties removed per directive
app.use('/api/users', userRoutes);
app.use('/api/merchant', merchantRoutes);

// SMS webhook endpoint
app.use('/webhook/sms', require('./routes/smsWebhook'));

// Health check endpoint
app.get('/health', (req, res) => {
  res.json({ status: 'healthy', timestamp: new Date().toISOString() });
});

// Health check for Sentinel connectivity
app.get('/health/sentinel', async (req, res) => {
  try {
    const SENTINEL_URL = process.env.SENTINEL_URL || 'http://inference:3002';
    const SENTINEL_AUTH_TOKEN = process.env.SENTINEL_AUTH_TOKEN || '';
    // Minimal ping: send a tiny inference payload; inference service should respond quickly
    const payload = {
      coupon_hash: '0'.repeat(64),
      kid: '00000000',
      expiry_ts: Date.now() + 60000,
      seal: '00000000',
      grid_id: 'health',
      amount: 0.01
    };
    const resp = await axios.post(`${SENTINEL_URL}/inference`, payload, {
      timeout: 800,
      headers: {
        'Content-Type': 'application/json',
        ...(SENTINEL_AUTH_TOKEN ? { 'X-Mari-Auth': SENTINEL_AUTH_TOKEN } : {})
      }
    });
    res.json({ ok: true, status: 'reachable', score: resp.data?.score ?? null, modelId: resp.data?.model_id ?? null });
  } catch (e) {
    res.status(503).json({ ok: false, status: 'unreachable', error: e?.message || 'unknown' });
  }
});

// Error handling
app.use(errorHandler);

module.exports = app;
