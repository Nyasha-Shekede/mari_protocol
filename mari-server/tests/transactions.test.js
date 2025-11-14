// tests/transactions.test.js
const request = require('supertest');
const crypto = require('crypto');
const app = require('../src/app');

// Helper to make base64(SPKI) for ECDSA P-256
async function generateDeviceKey() {
  return new Promise((resolve, reject) => {
    crypto.generateKeyPair('ec', { namedCurve: 'P-256' }, (err, pub, priv) => {
      if (err) return reject(err);
      try {
        const spkiDer = pub.export({ type: 'spki', format: 'der' });
        const spkiB64 = Buffer.from(spkiDer).toString('base64');
        resolve({ pub, priv, spkiB64 });
      } catch (e) { reject(e); }
    });
  });
}

function canonical(obj) {
  const ordered = {};
  Object.keys(obj).sort().forEach(k => { ordered[k] = obj[k]; });
  return JSON.stringify(ordered);
}

function sign(priv, payload) {
  const sign = crypto.createSign('SHA256');
  sign.update(Buffer.from(payload));
  sign.end();
  // Try ieee-p1363 raw first; fallback to DER
  try { return sign.sign({ key: priv, dsaEncoding: 'ieee-p1363' }).toString('base64'); } catch {}
  return sign.sign(priv).toString('base64');
}

function makeCoupon({ from, to, amount, grid, seal = '1a2b3c4d' }) {
  const exp = Date.now() + 5*60*1000;
  const params = new URLSearchParams();
  params.append('from', from);
  params.append('to', to);
  params.append('val', Number.isInteger(amount) ? String(amount) : amount.toFixed(2));
  params.append('g', grid);
  params.append('exp', String(exp));
  params.append('s', seal);
  return `mari://xfer?${params.toString()}`;
}

describe('Transactions Gatekeeper', () => {
  it('registers device and accepts a signed transaction (happy path)', async () => {
    const { priv, spkiB64 } = await generateDeviceKey();
    const kid = 'a1b2c3d4';

    // Register device
    const reg = await request(app)
      .post('/api/transactions/register-device')
      .send({ kid, spki: spkiB64 });
    expect(reg.statusCode).toBe(200);
    expect(reg.body.ok).toBe(true);

    // Prepare signed transaction
    const from = '1001', to = '1002', amount = 25, grid = 'grid123';
    const coupon = makeCoupon({ from, to, amount, grid });
    const payload = { from, to, amount, grid, coupon };
    const sigB64 = sign(priv, canonical(payload));

    const tx = await request(app)
      .post('/api/transactions')
      .send({ ...payload, kid, sig: sigB64 });

    expect(tx.statusCode).toBe(200);
    expect(tx.body.ok).toBe(true);
    expect(tx.body).toHaveProperty('couponHash');
    // incrementKey is included from HSM mock path
    expect(tx.body).toHaveProperty('incrementKey');
  });

  it('rejects invalid signature', async () => {
    const { spkiB64 } = await generateDeviceKey();
    const { priv: otherPriv } = await generateDeviceKey();
    const kid = 'deadbeef';

    await request(app).post('/api/transactions/register-device').send({ kid, spki: spkiB64 });

    const from = '1001', to = '1002', amount = 10, grid = 'gridX';
    const coupon = makeCoupon({ from, to, amount, grid });
    const payload = { from, to, amount, grid, coupon };
    // Sign with a different private key -> invalid
    const sigB64 = sign(otherPriv, canonical(payload));

    const tx = await request(app).post('/api/transactions').send({ ...payload, kid, sig: sigB64 });
    expect(tx.statusCode).toBe(400);
    expect(tx.body.error).toMatch(/Invalid signature/i);
  });

  it('rejects invalid seal format', async () => {
    const { priv, spkiB64 } = await generateDeviceKey();
    const kid = 'ffeeddcc';
    await request(app).post('/api/transactions/register-device').send({ kid, spki: spkiB64 });

    const from = '1001', to = '1002', amount = 5, grid = 'gridY';
    const badCoupon = makeCoupon({ from, to, amount, grid, seal: 'xyz' }); // not 8-hex
    const payload = { from, to, amount, grid, coupon: badCoupon };
    const sigB64 = sign(priv, canonical(payload));
    const tx = await request(app).post('/api/transactions').send({ ...payload, kid, sig: sigB64 });
    expect(tx.statusCode).toBe(400);
    expect(tx.body.error).toMatch(/Invalid seal format/i);
  });

  it('rejects duplicate coupons (idempotency)', async () => {
    const { priv, spkiB64 } = await generateDeviceKey();
    const kid = '11223344';
    await request(app).post('/api/transactions/register-device').send({ kid, spki: spkiB64 });

    const from = '1001', to = '1002', amount = 7, grid = 'gridZ';
    const coupon = makeCoupon({ from, to, amount, grid });
    const payload = { from, to, amount, grid, coupon };
    const sigB64 = sign(priv, canonical(payload));

    const first = await request(app).post('/api/transactions').send({ ...payload, kid, sig: sigB64 });
    expect(first.statusCode).toBe(200);

    const dup = await request(app).post('/api/transactions').send({ ...payload, kid, sig: sigB64 });
    expect(dup.statusCode).toBe(409);
    expect(dup.body.error).toMatch(/Duplicate coupon/i);
  });
});
