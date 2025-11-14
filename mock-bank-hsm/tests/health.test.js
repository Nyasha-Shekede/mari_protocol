const request = require('supertest');
const app = require('../src/app');

describe('Health endpoint', () => {
  it('GET /health should return OK status and service name', async () => {
    const res = await request(app).get('/health');
    expect(res.statusCode).toBe(200);
    expect(res.body).toHaveProperty('status', 'OK');
    expect(res.body).toHaveProperty('service', 'Mari Mock Bank');
    expect(res.body).toHaveProperty('timestamp');
  });
});
