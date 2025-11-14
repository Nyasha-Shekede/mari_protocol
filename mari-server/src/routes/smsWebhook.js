// src/routes/smsWebhook.js
const express = require('express');
const router = express.Router();
const SMSProcessor = require('../services/smsProcessor');
const { validate, Joi } = require('../middleware/validate');
const crypto = require('crypto');

// Optional Twilio signature verification
function verifyTwilioSignature(req, res, next) {
  const authToken = process.env.TWILIO_AUTH_TOKEN;
  if (!authToken) return next(); // not configured; skip verification
  const signature = req.get('X-Twilio-Signature') || req.get('x-twilio-signature');
  if (!signature) return res.status(403).json({ success: false, error: 'missing_twilio_signature' });

  try {
    const url = `${req.protocol}://${req.get('host')}${req.originalUrl}`;
    const params = req.body || {};
    const signed = Object.keys(params)
      .sort()
      .reduce((acc, k) => acc + k + (params[k] ?? ''), url);
    const expected = crypto.createHmac('sha1', authToken).update(signed).digest('base64');
    if (crypto.timingSafeEqual(Buffer.from(signature), Buffer.from(expected))) return next();
  } catch {}
  return res.status(403).json({ success: false, error: 'invalid_twilio_signature' });
}

// Normalize incoming payload to a common shape
function normalizeSmsPayload(req, _res, next) {
  const b = req.body || {};
  // Twilio form fields: From, To, Body
  const from = b.from || b.From || b.sender || b.Sender;
  const to = b.to || b.To || b.recipient || b.Recipient;
  const body = b.body || b.Body || b.message || b.Message;
  req.body = {
    from,
    to,
    body,
    timestamp: b.timestamp || b.Timestamp || undefined,
  };
  next();
}

router.post('/incoming', verifyTwilioSignature, normalizeSmsPayload, validate({ body: Joi.object({
  from: Joi.string().required(),
  to: Joi.string().required(),
  body: Joi.string().required(),
  timestamp: Joi.any().optional()
}) }), async (req, res) => {
  try {
    const { from, to, body, timestamp } = req.body;
    
    // Process Mari SMS
    const result = await SMSProcessor.processIncomingSMS({
      from,
      to,
      body,
      timestamp
    });
    
    res.json({ success: true, data: result });
  } catch (error) {
    console.error('SMS Processing Error:', error);
    res.status(400).json({ success: false, error: error.message });
  }
});

router.post('/status', validate({ body: Joi.object({
  messageId: Joi.string().required(),
  status: Joi.string().required()
}) }), (req, res) => {
  // Handle SMS delivery status updates
  const { messageId, status } = req.body;
  console.log(`SMS ${messageId} status: ${status}`);
  res.json({ success: true });
});

module.exports = router;
