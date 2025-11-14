// src/routes/settlement.js
// Core-facing settlement API that proxies to mock-bank-hsm

const express = require('express');
const router = express.Router();
const { validate, Joi } = require('../middleware/validate');
const BankClient = require('../services/bankClient');

const bankClient = new BankClient();

// POST /api/settlement/process
// Matches Mari app SettlementRequest shape and forwards to mock bank
router.post('/process', validate({ body: Joi.object({
  batchId: Joi.string().required(),
  merchantId: Joi.string().required(), // Core merchant id (for audit)
  bankMerchantId: Joi.string().required(), // Bank-side merchant account id
  seal: Joi.string().required(),
  transactions: Joi.array().items(
    Joi.object({
      id: Joi.string().required(),
      amount: Joi.number().positive().required(),
      coupon: Joi.string().required(),
      physicsData: Joi.object().optional() // currently ignored by bank
    })
  ).min(1).required()
}) }), async (req, res) => {
  try {
    const { batchId, merchantId, bankMerchantId, seal, transactions } = req.body;

    // Bank expects merchantId to be the bank-side id
    const bankPayload = {
      batchId,
      merchantId: bankMerchantId,
      seal,
      transactions: transactions.map(t => ({ id: t.id, amount: t.amount, coupon: t.coupon }))
    };

    const result = await bankClient.processSettlement(bankPayload);

    // BankClient returns the mock-bank JSON ({ success, data })
    if (result && result.success && result.data) {
      return res.json({ success: true, data: result.data });
    }

    return res.status(502).json({ success: false, error: 'Invalid settlement response from bank', detail: result });
  } catch (e) {
    res.status(500).json({ success: false, error: e.message });
  }
});

module.exports = router;
