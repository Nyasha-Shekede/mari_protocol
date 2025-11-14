// src/routes/transactions.js (Phase 2: unified signed transaction API + device key registration)
const express = require('express');
const crypto = require('crypto');
const router = express.Router();
const axios = require('axios');
const { v4: uuidv4 } = require('uuid');
const Transaction = require('../models/Transaction');
const PhysicsValidationService = require('../services/physicsValidationService');
const { publishOutcome/*, publishPre*/ } = require('../services/labelPublisher');

const SENTINEL_URL = process.env.SENTINEL_URL || 'http://inference:3002';
const SENTINEL_THRESHOLD = Number(process.env.SENTINEL_THRESHOLD || 850);
const SENTINEL_AUTH_TOKEN = process.env.SENTINEL_AUTH_TOKEN || '';
const SENTINEL_FAIL_OPEN = String(process.env.SENTINEL_FAIL_OPEN || 'false').toLowerCase() === 'true';
const { validate, Joi } = require('../middleware/validate');

// In-memory idempotency cache and device registry (mock)
const { getSigningKey, setSigningKey, setUserEncKey } = require('../services/deviceRegistry');
const seenCoupons = new Set(); // coupon hash strings

const canonicalize = (obj) => JSON.stringify(obj, Object.keys(obj).sort());
const sha256Hex = (s) => crypto.createHash('sha256').update(s).digest('hex');

router.post('/register-device', validate({ body: Joi.object({
  kid: Joi.string().length(8).required(),
  spki: Joi.string().required(),
  // Optional encryption public key (RSA-OAEP SPKI base64) and user id to map
  encSpki: Joi.string().optional(),
  userId: Joi.string().optional()
}) }), async (req, res) => {
  const { kid, spki, encSpki, userId } = req.body;
  setSigningKey(kid, spki);
  if (encSpki && userId) setUserEncKey(userId, encSpki);
  res.json({ ok: true });
});

// Unified transaction intake (HTTP/SMS normalized)
// Supports both the legacy app shape and the new signed payload shape.
router.post('/', validate({ body: Joi.object({
  // New shape
  from: Joi.string(),
  to: Joi.string(),
  grid: Joi.string(),
  kid: Joi.string().length(8),
  sig: Joi.string(),

  // Legacy app shape
  senderBioHash: Joi.string(),
  receiverBioHash: Joi.string(),
  locationGrid: Joi.string(),

  // Shared fields
  amount: Joi.number().positive().required(),
  coupon: Joi.string().required(),
  physicsData: Joi.object({
    location: Joi.object({
      grid: Joi.string().required()
    }).required(),
    motion: Joi.object({
      x: Joi.number().required(),
      y: Joi.number().required(),
      z: Joi.number().required()
    }).required(),
    timestamp: Joi.alternatives().try(Joi.string(), Joi.date(), Joi.number()).required()
  }).optional()
}) }), async (req, res) => {
  try {
    const body = req.body;
    const from = body.from || body.senderBioHash;
    const to = body.to || body.receiverBioHash;
    const grid = body.grid || body.locationGrid;
    const { amount, coupon, kid, sig, physicsData } = body;

    if (!from || !to || !grid) {
      return res.status(400).json({ error: 'Missing sender/receiver/grid' });
    }

    // Idempotency: coupon hash (per-process). In a production system this would be persisted.
    const couponHash = sha256Hex(coupon);
    if (seenCoupons.has(couponHash)) {
      return res.status(409).json({ error: 'Duplicate coupon' });
    }

    // Verify signature using device public key from in-memory registry when present
    if (kid && sig) {
      const spkiB64 = getSigningKey(kid);
      if (!spkiB64) return res.status(400).json({ error: 'Unknown device key id' });
      const spkiBuf = Buffer.from(spkiB64, 'base64');
      const pubKey = crypto.createPublicKey({ key: spkiBuf, format: 'der', type: 'spki' });
      const txPayload = { from, to, amount, grid, coupon };
      const canonical = canonicalize(txPayload);
      const sigBuf = Buffer.from(sig, 'base64');
      const ok = crypto.verify('sha256', Buffer.from(canonical), { key: pubKey, dsaEncoding: 'ieee-p1363' }, sigBuf)
        || crypto.verify('sha256', Buffer.from(canonical), pubKey, sigBuf);
      if (!ok) return res.status(400).json({ error: 'Invalid signature' });
    }

    // Optional physics validation when physicsData is provided by the client
    if (physicsData) {
      const physicsCurrent = {
        location: {
          grid: physicsData.location?.grid || grid
        },
        motion: physicsData.motion
      };
      const validation = await PhysicsValidationService.validateTransaction(coupon, physicsCurrent, from);
      if (!validation.isValid) {
        return res.status(400).json({
          error: 'Physics validation failed',
          validation
        });
      }
    }

    // Basic seal format check (8 hex chars) and extract expiry if available
    try {
      const u = new URL(coupon);
      const s = u.searchParams.get('s') || '';
      if (!/^[0-9a-fA-F]{8}$/.test(s)) return res.status(400).json({ error: 'Invalid seal format' });
      // Pre-inference payload for Sentinel
      const exp = Number(u.searchParams.get('exp') || Date.now());
      const sentinelPayload = {
        coupon_hash: couponHash,
        kid,
        expiry_ts: exp,
        seal: s,
        grid_id: grid,
        amount
      };

      // Optional: publish PRE_SETTLEMENT (disabled by default to avoid noise)
      // await publishPre({ event_id: uuidv4(), event_type: 'PRE_SETTLEMENT', ts: Date.now(), ...sentinelPayload });

      // Call Sentinel with short timeout and 1 retry
      let sentinelScore = null, modelId = null, lastErr = null;
      for (let attempt = 0; attempt < 2; attempt++) {
        try {
          const resp = await axios.post(`${SENTINEL_URL}/inference`, sentinelPayload, {
            timeout: 600,
            headers: {
              'Content-Type': 'application/json',
              ...(SENTINEL_AUTH_TOKEN ? { 'X-Mari-Auth': SENTINEL_AUTH_TOKEN } : {})
            }
          });
          sentinelScore = resp.data?.score;
          modelId = resp.data?.model_id;
          break;
        } catch (e) {
          lastErr = e;
          if (attempt === 0) continue; // one retry
        }
      }
      if (sentinelScore == null) {
        if (!SENTINEL_FAIL_OPEN) {
          // Fail-close (default)
          await publishOutcome({
            event_id: uuidv4(), event_type: 'SETTLEMENT_OUTCOME', ts: Date.now(),
            ...sentinelPayload, result: 'REJECTED_BY_SENTINEL'
          }).catch(()=>{});
          return res.status(503).json({ error: 'sentinel_unavailable' });
        } else {
          // Fail-open: proceed without Sentinel score
          console.warn(`[SENTINEL] Unreachable; proceeding due to SENTINEL_FAIL_OPEN=true`);
          sentinelScore = 'NA';
        }
      }

      if (Number(sentinelScore) > SENTINEL_THRESHOLD) {
        await publishOutcome({
          event_id: uuidv4(), event_type: 'SETTLEMENT_OUTCOME', ts: Date.now(),
          ...sentinelPayload, result: 'REJECTED_BY_SENTINEL'
        }).catch(()=>{});
        return res.status(409).json({ error: 'high_risk_transaction', score: sentinelScore, modelId });
      }
    } catch {
      return res.status(400).json({ error: 'Invalid coupon URL' });
    }

    seenCoupons.add(couponHash);

    // Call Mock Bank HSM to produce a signed increment payload consistent with SMS path
    const hsmBase = process.env.BANK_BASE_URL || 'http://host.docker.internal:3001';
    const timeNs = Date.now() * 1e6;
    const hsmRes = await fetch(`${hsmBase}/api/hsm/increment-key`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: from, amount, couponHash, timeNs })
    }).then(r=>r.json()).catch(()=>({}));
    const payload = hsmRes?.data?.payload || null;
    const SIG = hsmRes?.data?.SIG || null;

    // Publish settlement outcome label (SUCCESS or ERROR)
    try {
      const u2 = new URL(coupon);
      const s2 = u2.searchParams.get('s') || '';
      const exp2 = Number(u2.searchParams.get('exp') || Date.now());
      await publishOutcome({
        event_id: uuidv4(), event_type: 'SETTLEMENT_OUTCOME', ts: Date.now(),
        coupon_hash: couponHash, kid, expiry_ts: exp2, seal: s2, grid_id: grid, amount,
        result: payload && SIG ? 'SUCCESS' : 'ERROR'
      });
    } catch {}

    // Persist transaction record for auditability (best-effort)
    try {
      await Transaction.create({
        transactionId: `TXN_${Date.now()}_${uuidv4()}`,
        senderBioHash: from,
        receiverBioHash: to,
        amount,
        locationGrid: grid,
        coupon,
        physicsData: physicsData ? {
          location: {
            grid: physicsData.location?.grid || grid
          },
          motion: physicsData.motion,
          timestamp: new Date(physicsData.timestamp)
        } : undefined,
        transportMethod: 'HTTP',
        status: payload && SIG ? 'SETTLED' : 'FAILED'
      });
    } catch (persistErr) {
      console.error('Failed to persist transaction', persistErr);
    }

    res.json({ ok: true, couponHash, payload, SIG });
  } catch (e) {
    res.status(500).json({ error: e.message });
  }
});

module.exports = router;
