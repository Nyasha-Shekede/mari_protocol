// src/services/smsProcessor.js
const crypto = require('crypto');
const Transaction = require('../models/Transaction');
const { sendSms } = require('./twilioClient');

class SMSProcessor {
  static async processIncomingSMS(smsData) {
    const { from, body } = smsData;
    
    // Support legacy envelope MARI_SMS:<base64-json> and plain mari://xfer coupons
    if (typeof body === 'string' && body.startsWith('MARI_SMS:')) {
      return this.processMariSMS(from, body);
    }

    if (typeof body === 'string' && body.toLowerCase().startsWith('mari://')) {
      return this.processMariCoupon(from, body);
    }
    
    throw new Error('Non-Mari SMS received');
  }

  static async processMariSMS(from, body) {
    // Legacy support: Extract and decrypt Mari transaction data
    const encryptedData = body.replace('MARI_SMS:', '');
    const transactionData = this.decryptSMSData(encryptedData);
    return this.processCouponObject(transactionData);
  }

  static async processMariCoupon(from, coupon) {
    // Parse plain mari URI from SMS Body
    let senderBioHash, receiverBioHash, amount, locationGrid;
    try {
      const u = new URL(coupon);
      senderBioHash = u.searchParams.get('from');
      receiverBioHash = u.searchParams.get('to');
      amount = Number(u.searchParams.get('val'));
      locationGrid = u.searchParams.get('g') || 'grid';
    } catch {
      throw new Error('Invalid mari coupon');
    }

    const transactionData = {
      id: `SMS_${Date.now()}`,
      senderBioHash,
      receiverBioHash,
      amount,
      locationGrid,
      coupon,
      physicsData: undefined
    };

    return this.processCouponObject(transactionData);
  }

  static decryptSMSData(encryptedData) {
    // Base64 decode and decrypt
    const decoded = Buffer.from(encryptedData, 'base64').toString('utf8');
    // In production, this would use proper decryption
    return JSON.parse(decoded);
  }

  static encryptSMSData(data) {
    // Encrypt and base64 encode data for SMS
    const jsonData = JSON.stringify(data);
    return Buffer.from(jsonData).toString('base64');
  }

  static sha256Hex(s) { return crypto.createHash('sha256').update(s).digest('hex'); }

  static async processCouponObject(transactionData) {
    // Compute couponHash and call Bank HSM for increment key
    const coupon = transactionData.coupon;
    const couponHash = this.sha256Hex(coupon);
    const timeNs = Date.now() * 1e6;
    const bankBase = process.env.BANK_BASE_URL || 'http://host.docker.internal:3001';

    let payload = null, SIG = null, settled = false;
    try {
      const res = await fetch(`${bankBase}/api/hsm/increment-key`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId: transactionData.senderBioHash, amount: transactionData.amount, couponHash, timeNs })
      }).then(r=>r.json());
      payload = res?.data?.payload || null;
      SIG = res?.data?.SIG || null;
      settled = Boolean(payload && SIG);
    } catch (_) { /* leave as PENDING if bank unreachable */ }

    const transaction = new Transaction({
      transactionId: transactionData.id || `SMS_${Date.now()}`,
      senderBioHash: transactionData.senderBioHash,
      receiverBioHash: transactionData.receiverBioHash,
      amount: transactionData.amount,
      locationGrid: transactionData.locationGrid,
      coupon: transactionData.coupon,
      transportMethod: 'SMS',
      status: settled ? 'SETTLED' : 'PENDING',
      physicsData: transactionData.physicsData
    });

    await transaction.save();

    // If settled and Twilio is configured, send short SMS receipts to payer and payee
    if (settled) {
      const prefix = process.env.TWILIO_E164_PREFIX || '';
      const fmt = (n) => {
        if (!n) return null;
        if (n.startsWith('+')) return n;
        return prefix ? `${prefix}${n}` : n;
      };
      const toPayer = fmt(transactionData.senderBioHash);
      const toPayee = fmt(transactionData.receiverBioHash);
      const amountStr = String(transactionData.amount);
      const shortHash = couponHash.slice(0,8);
      const msgPayer = `MARI_RCPT:amt=${amountStr};hash=${shortHash};role=payer`;
      const msgPayee = `MARI_RCPT:amt=${amountStr};hash=${shortHash};role=payee`;
      try { if (toPayer) await sendSms({ to: toPayer, body: msgPayer }); } catch {}
      try { if (toPayee) await sendSms({ to: toPayee, body: msgPayee }); } catch {}

      // Send cryptographic proof as multipart (Base64 of JSON { payload, SIG })
      const proofJson = JSON.stringify({ payload, SIG });
      const proofB64 = Buffer.from(proofJson).toString('base64');
      const chunkSize = 120; // leave room for prefix and indices
      const totalParts = Math.ceil(proofB64.length / chunkSize);
      for (let i = 0; i < totalParts; i++) {
        const chunk = proofB64.slice(i * chunkSize, (i + 1) * chunkSize);
        const partPayer = `MARI_RCPTP_PART:${i + 1}/${totalParts}:${chunk}`;
        const partPayee = `MARI_RCPTP_PART:${i + 1}/${totalParts}:${chunk}`;
        try { if (toPayer) await sendSms({ to: toPayer, body: partPayer }); } catch {}
        try { if (toPayee) await sendSms({ to: toPayee, body: partPayee }); } catch {}
      }
    }

    return {
      success: true,
      transactionId: transaction.transactionId,
      status: transaction.status,
      couponHash,
      payload,
      SIG
    };
  }
}

module.exports = SMSProcessor;
