require('dotenv').config();

module.exports = {
  port: process.env.PORT || 3001,
  bank: {
    name: process.env.BANK_NAME || 'Mari Demo Bank',
    code: process.env.BANK_CODE || 'MARI',
    country: process.env.BANK_COUNTRY || 'US',
    currency: process.env.BANK_CURRENCY || 'USD'
  },
  hsm: {
    privateKey: process.env.HSM_PRIVATE_KEY || 'demo-private-key-12345',
    publicKey: process.env.HSM_PUBLIC_KEY || 'demo-public-key-12345'
  },
  commissions: {
    // Support both legacy and new env names
    protocol: parseFloat(process.env.PROTOCOL_COMMISSION_RATE || process.env.COMMISSION_PROTOCOL) || 0.002,
    bank: parseFloat(process.env.BANK_COMMISSION_RATE || process.env.COMMISSION_BANK) || 0.003
  },
  demo: {
    initialBalance: parseFloat(process.env.DEMO_INITIAL_BALANCE) || 10000,
    initialReserve: parseFloat(process.env.DEMO_INITIAL_RESERVE) || 5000
  }
};
