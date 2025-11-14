// src/services/physicsValidationService.js
const crypto = require('crypto');
const config = require('../config');
const { MariStringParser } = require('@mari/shared-libs');

class PhysicsValidationService {
  constructor() {
    this.tolerance = config.security.physicsValidation.tolerance;
  }

  async validateTransaction(coupon, currentPhysics, currentBioHash) {
    const errors = [];
    let params;
    try {
      const parsed = MariStringParser.parseTransferCoupon(coupon);
      params = {
        exp: parsed.expiry,
        g: parsed.grid,
        s: parsed.seal,
        b: parsed.senderBio // using sender bio as blood hash analogue
      };
    } catch (e) {
      errors.push({ type: 'COUPON_PARSE_ERROR' });
      return { isValid: false, errors };
    }

    // Check expiration
    if (parseInt(params.exp) < Date.now()) {
      errors.push({ type: 'TIME_EXPIRED' });
    }

    // Check location grid (with tolerance for demo)
    if (!this.validateLocation(params.g, currentPhysics.location)) {
      errors.push({ type: 'LOCATION_MISMATCH' });
    }

    // Check motion seal (simplified for demo)
    if (!this.validateMotionSeal(params.s, currentPhysics.motion)) {
      errors.push({ type: 'MOTION_MISMATCH' });
    }

    // Check blood seal (simplified for demo), only when current bio hash is provided
    const bio = currentBioHash ?? currentPhysics?.bioHash;
    if (bio) {
      if (!this.validateBloodSeal(params.b, bio)) {
        errors.push({ type: 'BLOOD_MISMATCH' });
      }
    }

    return {
      isValid: errors.length === 0,
      errors
    };
  }

  validateLocation(couponGrid, currentLocation) {
    // In production, this would use proper geohash comparison
    // For demo, we'll allow exact match
    return couponGrid === currentLocation.grid;
  }

  validateMotionSeal(couponSeal, currentMotion) {
    // Simplified validation - in real implementation, this would use
    // proper motion pattern matching
    const motionHash = this.generateMotionHash(currentMotion);
    return Math.abs(parseFloat(couponSeal) - motionHash) < 0.1;
  }

  validateBloodSeal(couponBlood, currentBioHash) {
    // Simplified blood validation
    return couponBlood === currentBioHash;
  }

  parseMariString(mariString) {
    // Deprecated: use MariStringParser instead
    const { parseTransferCoupon } = MariStringParser;
    const parsed = parseTransferCoupon(mariString);
    return {
      exp: parsed.expiry,
      g: parsed.grid,
      s: parsed.seal,
      b: parsed.senderBio
    };
  }

  generateMotionHash(motion) {
    // Generate a hash from motion data
    const motionString = `${motion.x},${motion.y},${motion.z}`;
    return parseInt(crypto.createHash('md5').update(motionString).digest('hex').substring(0, 8), 16) / 100000000;
  }
}

module.exports = new PhysicsValidationService();
