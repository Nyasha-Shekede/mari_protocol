// src/config/index.js
require('dotenv').config();

module.exports = {
  port: process.env.PORT || 3000,
  database: {
    url: process.env.DATABASE_URL || 'mongodb://localhost:27017/mari-core'
  },
  bank: {
    // Use BANK_BASE_URL for consistency with docker-compose and .env
    baseUrl: process.env.BANK_BASE_URL || 'http://mari-mock-bank:3001'
  },
  sms: {
    provider: process.env.SMS_PROVIDER || 'twilio',
    twilio: {
      accountSid: process.env.TWILIO_ACCOUNT_SID,
      authToken: process.env.TWILIO_AUTH_TOKEN,
      fromNumber: process.env.TWILIO_FROM_NUMBER
    }
  },
  security: {
    jwtSecret: process.env.JWT_SECRET || 'mari-secret-key',
    jwtExpiry: process.env.JWT_EXPIRY || '24h',
    bcryptRounds: parseInt(process.env.BCRYPT_ROUNDS) || 12,
    physicsValidation: {
      tolerance: {
        location: parseInt(process.env.LOCATION_TOLERANCE) || 100, // meters
        time: parseInt(process.env.TIME_TOLERANCE) || 300000 // 5 minutes
      }
    }
  }
};
