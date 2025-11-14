// src/middleware/physicsValidation.js
const PhysicsValidationService = require('../services/physicsValidationService');

const physicsValidation = async (req, res, next) => {
  try {
    const { coupon, physicsData } = req.body;

    if (!coupon || !physicsData) {
      return res.status(400).json({
        success: false,
        error: 'Missing coupon or physics data'
      });
    }

    const validation = await PhysicsValidationService.validateTransaction(coupon, physicsData);

    if (!validation.isValid) {
      req.physicsValidation = validation;
      return res.status(400).json({
        success: false,
        error: 'Physics validation failed',
        validation
      });
    }

    req.physicsValidation = validation;
    next();
  } catch (error) {
    res.status(500).json({
      success: false,
      error: 'Physics validation error'
    });
  }
};

module.exports = { physicsValidation };
