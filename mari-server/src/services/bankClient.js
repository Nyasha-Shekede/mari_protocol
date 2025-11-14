// src/services/bankClient.js
const axios = require('axios');
const config = require('../config');

class BankClient {
  constructor() {
    this.baseUrl = config.bank.baseUrl.replace(/\/$/, '');
    this.client = axios.create({ baseURL: this.baseUrl, timeout: 10000 });
  }

  async generateIncrementKey(payload) {
    try {
      const res = await this.client.post('/api/hsm/increment-key', payload);
      return res.data; // { success, data: incrementKeyObject|string }
    } catch (err) {
      const detail = err.response?.data || err.message;
      throw new Error(typeof detail === 'string' ? detail : (detail.error || JSON.stringify(detail)));
    }
  }

  async processSettlement(batchData) {
    try {
      const res = await this.client.post('/api/settlement/process', batchData);
      return res.data; // { success, data: { ... } }
    } catch (err) {
      const detail = err.response?.data || err.message;
      throw new Error(typeof detail === 'string' ? detail : (detail.error || JSON.stringify(detail)));
    }
  }

  async verifyCoupon(couponData) {
    try {
      const res = await this.client.post('/api/hsm/verify', couponData);
      return res.data;
    } catch (err) {
      const detail = err.response?.data || err.message;
      throw new Error(typeof detail === 'string' ? detail : (detail.error || JSON.stringify(detail)));
    }
  }
}

module.exports = BankClient;
