// src/utils/batchSeal.js
// Batch payment seal generation and validation
const crypto = require('crypto');

/**
 * Generate batch seal from list of (id, amount) pairs
 * Compatible with Android BatchSeal.kt implementation
 * @param {Array<{id: string, amount: number}>} items
 * @returns {string} SHA-256 hex hash
 */
function generateBatchSeal(items) {
  const parts = items.map(({ id, amount }) => {
    // Format amount: whole numbers without decimals, floats with decimals
    const amtStr = Number.isInteger(amount) ? amount.toString() : amount.toString();
    return id + amtStr;
  }).sort(); // Lexicographic sort for deterministic order
  
  const concat = parts.join('');
  return crypto.createHash('sha256').update(concat, 'utf8').digest('hex');
}

/**
 * Verify a batch seal matches the provided items
 * @param {string} seal - Expected batch seal (hex string)
 * @param {Array<{id: string, amount: number}>} items - Batch items
 * @returns {boolean}
 */
function verifyBatchSeal(seal, items) {
  const computed = generateBatchSeal(items);
  return computed === seal;
}

module.exports = {
  generateBatchSeal,
  verifyBatchSeal
};
