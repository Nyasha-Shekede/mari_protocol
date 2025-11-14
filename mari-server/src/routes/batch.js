// src/routes/batch.js
// Batch payment processing endpoint
const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { validate, Joi } = require('../middleware/validate');
const { generateBatchSeal, verifyBatchSeal } = require('../utils/batchSeal');
const { v4: uuidv4 } = require('uuid');

// In-memory batch tracking (production: use database)
const processedBatches = new Set();

/**
 * POST /api/batch/validate
 * Validate a batch seal before processing
 */
router.post('/validate', validate({ body: Joi.object({
  batchSeal: Joi.string().length(64).required(), // SHA-256 hex
  items: Joi.array().items(Joi.object({
    id: Joi.string().required(),
    amount: Joi.number().positive().required()
  })).min(1).required()
}) }), async (req, res) => {
  try {
    const { batchSeal, items } = req.body;
    
    // Verify seal matches items
    const isValid = verifyBatchSeal(batchSeal, items);
    
    if (!isValid) {
      return res.status(400).json({ 
        error: 'Invalid batch seal',
        message: 'Computed seal does not match provided seal'
      });
    }
    
    res.json({ 
      ok: true, 
      batchSeal,
      itemCount: items.length,
      totalAmount: items.reduce((sum, item) => sum + item.amount, 0)
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

/**
 * POST /api/batch/submit
 * Submit a batch of payments for processing
 */
router.post('/submit', validate({ body: Joi.object({
  batchId: Joi.string().optional(), // Optional client-provided batch ID
  batchSeal: Joi.string().length(64).required(),
  items: Joi.array().items(Joi.object({
    id: Joi.string().required(),
    amount: Joi.number().positive().required(),
    recipient: Joi.string().required() // Phone number or user ID
  })).min(1).max(1000).required(), // Limit batch size
  from: Joi.string().required(), // Sender phone/ID
  kid: Joi.string().length(8).required(), // Device key ID
  sig: Joi.string().required() // Signature over batch
}) }), async (req, res) => {
  try {
    const { batchId, batchSeal, items, from, kid, sig } = req.body;
    
    // Generate or use provided batch ID
    const finalBatchId = batchId || uuidv4();
    
    // Check for duplicate batch submission
    if (processedBatches.has(finalBatchId)) {
      return res.status(409).json({ 
        error: 'Duplicate batch',
        batchId: finalBatchId
      });
    }
    
    // Verify batch seal
    const sealItems = items.map(({ id, amount }) => ({ id, amount }));
    if (!verifyBatchSeal(batchSeal, sealItems)) {
      return res.status(400).json({ error: 'Invalid batch seal' });
    }
    
    // TODO: Verify signature (similar to single transaction flow)
    // For now, basic validation
    
    // Mark batch as processed
    processedBatches.add(finalBatchId);
    
    // In production: queue batch items for async processing
    // For now: immediate mock response
    const results = items.map(item => ({
      id: item.id,
      recipient: item.recipient,
      amount: item.amount,
      status: 'pending', // Would be 'queued' or 'processing' in production
      transactionId: uuidv4()
    }));
    
    res.json({
      ok: true,
      batchId: finalBatchId,
      batchSeal,
      itemCount: items.length,
      totalAmount: items.reduce((sum, item) => sum + item.amount, 0),
      results,
      status: 'batch_accepted'
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

/**
 * GET /api/batch/status/:batchId
 * Check status of a submitted batch
 */
router.get('/status/:batchId', async (req, res) => {
  try {
    const { batchId } = req.params;
    
    if (!processedBatches.has(batchId)) {
      return res.status(404).json({ error: 'Batch not found' });
    }
    
    // In production: query database for batch status
    res.json({
      ok: true,
      batchId,
      status: 'completed', // Mock status
      processedAt: new Date().toISOString()
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

/**
 * POST /api/batch/generate-seal
 * Helper endpoint to generate seal for testing
 */
router.post('/generate-seal', validate({ body: Joi.object({
  items: Joi.array().items(Joi.object({
    id: Joi.string().required(),
    amount: Joi.number().positive().required()
  })).min(1).required()
}) }), async (req, res) => {
  try {
    const { items } = req.body;
    const batchSeal = generateBatchSeal(items);
    
    res.json({
      ok: true,
      batchSeal,
      itemCount: items.length,
      totalAmount: items.reduce((sum, item) => sum + item.amount, 0)
    });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
