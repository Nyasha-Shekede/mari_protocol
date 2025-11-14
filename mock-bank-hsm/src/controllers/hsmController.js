const crypto = require('crypto');
const MockHsmService = require('../services/mockHsmService');

class HsmController {
  async generateIncrementKey(req, res) {
    try {
      const { userId, amount, couponHash, timeNs } = req.body;
      if (!userId || !amount || !couponHash || !timeNs) throw new Error('Missing fields');
      const result = MockHsmService.issueIncrementKey({ userId, amount, couponHash, timeNs });
      res.json({ success: true, data: result });
    } catch (error) {
      res.status(400).json({ success: false, error: error.message });
    }
  }

  async verifyIncrementKey(req, res) {
    try {
      const { payload, SIG } = req.body;
      if (!payload || !SIG) throw new Error('payload and SIG are required');
      const canonical = JSON.stringify(payload, Object.keys(payload).sort());
      const pub = MockHsmService.getPublicKey();
      const ok = crypto.verify('sha256', Buffer.from(canonical), pub.publicKeyPem, Buffer.from(SIG, 'base64'));
      res.json({ success: true, data: { isValid: !!ok } });
    } catch (error) {
      res.status(400).json({ success: false, error: error.message });
    }
  }

  async getPublicKey(req, res) {
    try {
      const publicKey = MockHsmService.getPublicKey();
      res.json({ success: true, data: publicKey });
    } catch (error) {
      res.status(400).json({ success: false, error: error.message });
    }
  }
}

module.exports = new HsmController();
