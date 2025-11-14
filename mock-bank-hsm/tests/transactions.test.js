const request = require('supertest');
const app = require('../src/app');

describe('Transactions API', () => {
  it('GET /api/transactions/history/:accountId returns mock history with pagination', async () => {
    const res = await request(app)
      .get('/api/transactions/history/acc_1')
      .query({ limit: 1, offset: 0 });

    expect(res.statusCode).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('transactions');
    expect(Array.isArray(res.body.data.transactions)).toBe(true);
    expect(res.body.data.transactions.length).toBe(1);
    expect(res.body.data).toHaveProperty('total');
    expect(res.body.data).toHaveProperty('accountId', 'acc_1');
  });

  it('GET /api/transactions/:transactionId returns mock transaction details', async () => {
    const res = await request(app).get('/api/transactions/txn_abc');

    expect(res.statusCode).toBe(200);
    expect(res.body.success).toBe(true);
    expect(res.body.data).toHaveProperty('id', 'txn_abc');
    expect(res.body.data).toHaveProperty('details');
  });
});
