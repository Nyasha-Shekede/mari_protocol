const express = require('express');
const router = express.Router();
const SettlementController = require('../controllers/settlementController');
const { validate, Joi } = require('../middleware/validate');

router.post('/process', validate({ body: Joi.object({
  batchId: Joi.string().required(),
  merchantId: Joi.string().required(),
  seal: Joi.string().required(),
  transactions: Joi.array().items(
    Joi.object({
      id: Joi.string().required(),
      amount: Joi.number().positive().required(),
      coupon: Joi.string().required()
    })
  ).min(1).required()
}) }), SettlementController.processSettlement);

module.exports = router;
