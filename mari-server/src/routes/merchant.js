// src/routes/merchant.js
const express = require('express');
const router = express.Router();
const { authMiddleware } = require('../middleware/auth');

// GET /api/merchant/profile
// Returns the core merchantId (userId) and an optional bankMerchantId from env for demo purposes
router.get('/profile', authMiddleware, async (req, res) => {
  try {
    const merchantId = req.user?.userId;
    const bankMerchantId = process.env.DEMO_BANK_MERCHANT_ID || '';
    return res.json({ success: true, data: { merchantId, bankMerchantId } });
  } catch (err) {
    return res.status(400).json({ success: false, error: err.message });
  }
});

module.exports = router;
