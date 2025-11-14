const express = require('express');
const router = express.Router();
const { validate, Joi } = require('../middleware/validate');

// Transaction history endpoint
router.get('/history/:accountId', validate({
  params: Joi.object({ accountId: Joi.string().required() }),
  query: Joi.object({ limit: Joi.number().integer().min(1).max(200).optional(), offset: Joi.number().integer().min(0).optional() })
}), (req, res) => {
  const { accountId } = req.params;
  const { limit = 50, offset = 0 } = req.query;
  
  // Mock transaction history
  const transactions = [
    {
      id: 'txn_123',
      type: 'SETTLEMENT',
      amount: 150.50,
      status: 'COMPLETED',
      timestamp: new Date().toISOString(),
      description: 'Mari settlement payment'
    },
    {
      id: 'txn_124',
      type: 'FEE',
      amount: -2.25,
      status: 'COMPLETED',
      timestamp: new Date().toISOString(),
      description: 'Processing fee'
    }
  ];

  res.json({
    success: true,
    data: {
      transactions: transactions.slice(offset, offset + limit),
      total: transactions.length,
      accountId
    }
  });
});

// Transaction details endpoint
router.get('/:transactionId', validate({ params: Joi.object({ transactionId: Joi.string().required() }) }), (req, res) => {
  const { transactionId } = req.params;
  
  const transaction = {
    id: transactionId,
    type: 'SETTLEMENT',
    amount: 150.50,
    status: 'COMPLETED',
    timestamp: new Date().toISOString(),
    description: 'Mari settlement payment',
    details: {
      merchantId: 'merchant_123',
      batchId: 'batch_456',
      commissions: {
        protocol: 0.30,
        bank: 0.45
      }
    }
  };

  res.json({
    success: true,
    data: transaction
  });
});

module.exports = router;
