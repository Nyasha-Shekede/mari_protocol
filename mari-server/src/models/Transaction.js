// src/models/Transaction.js
const mongoose = require('mongoose');

const transactionSchema = new mongoose.Schema({
  transactionId: {
    type: String,
    required: true,
    unique: true
  },
  senderBioHash: {
    type: String,
    required: true
  },
  receiverBioHash: {
    type: String,
    required: true
  },
  amount: {
    type: Number,
    required: true,
    min: 0
  },
  locationGrid: {
    type: String,
    required: true
  },
  timestamp: {
    type: Date,
    default: Date.now
  },
  status: {
    type: String,
    enum: ['PENDING', 'SETTLED', 'FAILED'],
    default: 'PENDING'
  },
  coupon: {
    type: String,
    required: true
  },
  transportMethod: {
    type: String,
    enum: ['SMS', 'HTTP'],
    required: true
  },
  physicsData: {
    location: {
      latitude: Number,
      longitude: Number,
      grid: String
    },
    motion: {
      x: Number,
      y: Number,
      z: Number
    },
    timestamp: Date
  }
}, {
  timestamps: true
});

transactionSchema.index({ transactionId: 1 });
transactionSchema.index({ senderBioHash: 1 });
transactionSchema.index({ receiverBioHash: 1 });
transactionSchema.index({ status: 1 });
transactionSchema.index({ createdAt: -1 });

module.exports = mongoose.model('Transaction', transactionSchema);
