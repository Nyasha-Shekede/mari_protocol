const request = require('supertest');
const app = require('../src/app');

describe('Accounts API', () => {
  let createdAccount;
  const bioHash = 'biohash_abc123';

  it('POST /api/accounts should create an account', async () => {
    const res = await request(app)
      .post('/api/accounts')
      .send({ name: 'Test Merchant', initialBalance: 0 });

    expect(res.statusCode).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('id');
    createdAccount = res.body.data;
  });

  it('GET /api/accounts/:id should return the created account', async () => {
    const res = await request(app)
      .get(`/api/accounts/${createdAccount.id}`)
      .send();

    expect(res.statusCode).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.id).toBe(createdAccount.id);
  });

  it('POST /api/accounts/reserve should create a reserve account', async () => {
    const res = await request(app)
      .post('/api/accounts/reserve')
      .send({ userId: 'user_1', bioHash, initialReserve: 1000 });

    expect(res.statusCode).toBe(201);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('id');
    expect(res.body.data.mariBioHash).toBe(bioHash);
  });

  it('GET /api/accounts/reserve/:bioHash should return the reserve account', async () => {
    const res = await request(app)
      .get(`/api/accounts/reserve/${bioHash}`)
      .send();

    expect(res.statusCode).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data.mariBioHash).toBe(bioHash);
  });
});
