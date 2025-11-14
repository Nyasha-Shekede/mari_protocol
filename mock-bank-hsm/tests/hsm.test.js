const request = require('supertest');
const app = require('../src/app');

describe('HSM API', () => {
  it('GET /api/hsm/public-key should return RSA public key info', async () => {
    const res = await request(app).get('/api/hsm/public-key');
    expect(res.statusCode).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('keyId');
    expect(res.body.data).toHaveProperty('publicKeyPem');
    expect(res.body.data).toHaveProperty('algorithm');
  });

  it('POST /api/hsm/increment-key should issue payload+SIG and /verify should accept it', async () => {
    const couponHash = 'a'.repeat(64);
    const timeNs = Date.now() * 1e6;
    const gen = await request(app)
      .post('/api/hsm/increment-key')
      .send({ userId: 'user_1', amount: 123.45, couponHash, timeNs });

    expect(gen.statusCode).toBe(200);
    expect(gen.body.success).toBe(true);
    expect(gen.body.data).toHaveProperty('payload');
    expect(gen.body.data).toHaveProperty('SIG');
    const { payload, SIG } = gen.body.data;
    expect(payload).toHaveProperty('USER_ID', 'user_1');
    expect(payload).toHaveProperty('AMOUNT');
    expect(payload).toHaveProperty('COUPON_HASH', couponHash);
    expect(payload).toHaveProperty('TIME_NS');
    expect(payload).toHaveProperty('VERSION');
    expect(payload).toHaveProperty('HSM_KID');

    const verify = await request(app)
      .post('/api/hsm/verify')
      .send({ payload, SIG });
    expect(verify.statusCode).toBe(200);
    expect(verify.body.success).toBe(true);
    expect(verify.body.data).toHaveProperty('isValid', true);
  });
});
