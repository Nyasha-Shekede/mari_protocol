// src/services/deviceRegistry.js
// Simple in-memory registry for device signing keys and encryption keys
// Not for production use.

const deviceKeys = new Map(); // kid -> spkiB64 (ECDSA verify)
const userEncKeys = new Map(); // userId(10-digit) -> encSpkiB64 (RSA-OAEP encrypt)

module.exports = {
  setSigningKey(kid, spkiB64) { deviceKeys.set(kid, spkiB64); },
  getSigningKey(kid) { return deviceKeys.get(kid); },
  setUserEncKey(userId, encSpkiB64) { if (userId) userEncKeys.set(userId, encSpkiB64); },
  getUserEncKey(userId) { return userEncKeys.get(userId); }
};
