const request = require('supertest');
const app = require('../src/app');

describe('HSM Increment Key Monotonicity', () => {
  it('issues increment keys with monotonic VERSION per USER_ID and valid signature', async () => {
    const userId = 'user_monotonic';
    const couponHash = 'b'.repeat(64);
    const t1 = Date.now() * 1e6;
    const r1 = await request(app).post('/api/hsm/increment-key').send({ userId, amount: 10, couponHash, timeNs: t1 });
    expect(r1.statusCode).toBe(200);
    expect(r1.body.success).toBe(true);
    const { payload: p1, SIG: s1 } = r1.body.data;
    expect(p1.USER_ID).toBe(userId);
    expect(p1.VERSION).toBeGreaterThanOrEqual(1);

    const verify1 = await request(app).post('/api/hsm/verify').send({ payload: p1, SIG: s1 });
    expect(verify1.statusCode).toBe(200);
    expect(verify1.body.data.isValid).toBe(true);

    // Second issuance should have VERSION strictly greater
    const t2 = t1 + 1000;
    const r2 = await request(app).post('/api/hsm/increment-key').send({ userId, amount: 5, couponHash, timeNs: t2 });
    const { payload: p2, SIG: s2 } = r2.body.data;
    expect(p2.VERSION).toBeGreaterThan(p1.VERSION);
    const verify2 = await request(app).post('/api/hsm/verify').send({ payload: p2, SIG: s2 });
    expect(verify2.body.data.isValid).toBe(true);

    // Tamper payload -> verify should fail
    const tampered = { ...p2, AMOUNT: p2.AMOUNT + 1 };
    const verifyTampered = await request(app).post('/api/hsm/verify').send({ payload: tampered, SIG: s2 });
    expect(verifyTampered.body.data.isValid).toBe(false);
  });
});
