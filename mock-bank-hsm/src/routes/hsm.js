const express = require('express');
const router = express.Router();
const HsmController = require('../controllers/hsmController');
const { validate, Joi } = require('../middleware/validate');

// Issue a signed increment key with TIME_NS and VERSION
router.post('/increment-key', validate({ body: Joi.object({
  userId: Joi.string().required(),
  amount: Joi.number().positive().required(),
  couponHash: Joi.string().length(64).hex().required(),
  timeNs: Joi.number().required()
}) }), HsmController.generateIncrementKey);

// Verify a signed increment key (payload + SIG)
router.post('/verify', validate({ body: Joi.object({
  payload: Joi.object({
    USER_ID: Joi.string().required(),
    AMOUNT: Joi.number().positive().required(),
    COUPON_HASH: Joi.string().length(64).hex().required(),
    TIME_NS: Joi.number().required(),
    VERSION: Joi.number().integer().min(1).required(),
    HSM_KID: Joi.string().required()
  }).required(),
  SIG: Joi.string().required()
}) }), HsmController.verifyIncrementKey);

router.get('/public-key', HsmController.getPublicKey);

module.exports = router;
