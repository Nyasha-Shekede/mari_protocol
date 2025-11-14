function validateMariCoupon(coupon) {
  if (!coupon || !coupon.startsWith('mari://')) {
    return {
      isValid: false,
      errors: ['Invalid coupon format']
    };
  }

  try {
    const urlParams = new URLSearchParams(coupon.split('?')[1]);
    const params = Object.fromEntries(urlParams.entries());
    
    const errors = [];
    
    if (!params.from) errors.push('Missing sender bio hash');
    if (!params.to) errors.push('Missing receiver bio hash');
    if (!params.val) errors.push('Missing amount');
    if (!params.g) errors.push('Missing location grid');
    if (!params.exp) errors.push('Missing expiration');
    if (!params.s) errors.push('Missing seal');
    
    if (params.exp && parseInt(params.exp) < Date.now()) {
      errors.push('Coupon expired');
    }
    
    if (params.val && isNaN(parseFloat(params.val))) {
      errors.push('Invalid amount');
    }
    
    return {
      isValid: errors.length === 0,
      senderBioHash: params.from,
      receiverBioHash: params.to,
      amount: parseFloat(params.val),
      locationGrid: params.g,
      expiry: parseInt(params.exp),
      seal: params.s,
      errors
    };
  } catch (error) {
    return {
      isValid: false,
      errors: [error.message]
    };
  }
}

module.exports = {
  validateMariCoupon
};
