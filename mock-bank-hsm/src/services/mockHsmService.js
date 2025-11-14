const { v4: uuidv4 } = require('uuid');
const crypto = require('crypto');

class MockHsmService {
  constructor() {
    // Generate an RSA keypair for mock signing if not provided
    if (process.env.HSM_PRIVATE_PEM && process.env.HSM_PUBLIC_PEM) {
      this.privateKeyPem = process.env.HSM_PRIVATE_PEM;
      this.publicKeyPem = process.env.HSM_PUBLIC_PEM;
    } else {
      const { privateKey, publicKey } = crypto.generateKeyPairSync('rsa', { modulusLength: 2048 });
      this.privateKeyPem = privateKey.export({ type: 'pkcs1', format: 'pem' });
      this.publicKeyPem = publicKey.export({ type: 'pkcs1', format: 'pem' });
    }
    this.keyId = uuidv4();
    this.userVersions = new Map(); // userId -> last VERSION
  }

  _canonical(obj) { return JSON.stringify(obj, Object.keys(obj).sort()); }

  issueIncrementKey({ userId, amount, couponHash, timeNs }) {
    const last = this.userVersions.get(userId) || 0;
    const VERSION = last + 1;
    const payload = {
      USER_ID: userId,
      AMOUNT: amount,
      COUPON_HASH: couponHash,
      TIME_NS: timeNs,
      VERSION,
      HSM_KID: this.keyId
    };
    const canonical = this._canonical(payload);
    const signature = crypto.sign('sha256', Buffer.from(canonical), this.privateKeyPem).toString('base64');
    this.userVersions.set(userId, VERSION);
    return { payload, SIG: signature };
  }

  getPublicKey() {
    return { keyId: this.keyId, algorithm: 'RSA-PSS-SHA256', publicKeyPem: this.publicKeyPem };
  }

  getPublicKeyByKid(kid) {
    // Single active key in mock; return if kid matches
    if (kid === this.keyId) return this.getPublicKey();
    return null;
  }
}

module.exports = new MockHsmService();
