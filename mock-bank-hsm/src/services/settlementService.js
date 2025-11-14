const BankAccountService = require('./bankAccountService');
const MockHsmService = require('./mockHsmService');
const { validateMariCoupon } = require('../utils/mariValidator');
const config = require('../config');
const { generateBatchSeal } = require('@mari/shared-libs');

class SettlementService {
  constructor() {
    this.commissionRates = {
      protocol: typeof config.commissions?.protocol === 'number' ? config.commissions.protocol : 0.002,
      bank: typeof config.commissions?.bank === 'number' ? config.commissions.bank : 0.003
    };
  }

  async processSettlementRequest(settlementData) {
    const { merchantId, transactions, batchId, seal } = settlementData || {};

    if (!merchantId) throw new Error('Missing merchantId');
    if (!batchId) throw new Error('Missing batchId');
    if (!Array.isArray(transactions) || transactions.length === 0) throw new Error('No transactions to process');
    if (!seal) throw new Error('Missing seal');
    if (!this.validateBatchSeal(transactions, seal)) throw new Error('Invalid batch seal');

    const merchantAccount = BankAccountService.getAccount(merchantId);
    if (!merchantAccount) {
      throw new Error('Merchant account not found');
    }

    const results = [];
    let totalAmount = 0;
    let totalCommission = 0;

    for (const transaction of transactions) {
      try {
        const validation = validateMariCoupon(transaction.coupon);
        
        if (validation.isValid) {
          // Ensure amount consistency and positivity
          const couponAmount = validation.amount;
          if (typeof couponAmount !== 'number' || !isFinite(couponAmount) || couponAmount <= 0) {
            results.push({
              transactionId: transaction.id,
              status: 'INVALID',
              errors: ['Invalid coupon amount']
            });
            continue;
          }

          if (typeof transaction.amount === 'number' && Math.abs(transaction.amount - couponAmount) > 1e-9) {
            results.push({
              transactionId: transaction.id,
              status: 'INVALID',
              errors: ['Transaction amount does not match coupon amount']
            });
            continue;
          }

          const amount = couponAmount;
          const { protocolCommission, bankCommission, netAmount } = this.calculateCommissions(amount);
          
          const transferResult = BankAccountService.processReserveTransfer(
            validation.senderBioHash,
            merchantId,
            netAmount
          );
          
          totalAmount += netAmount;
          totalCommission += (protocolCommission + bankCommission);
          
          results.push({
            transactionId: transaction.id,
            status: 'SETTLED',
            amount: netAmount,
            commissions: {
              protocol: protocolCommission,
              bank: bankCommission
            }
          });
        } else {
          results.push({
            transactionId: transaction.id,
            status: 'INVALID',
            errors: validation.errors
          });
        }
      } catch (error) {
        results.push({
          transactionId: transaction.id,
          status: 'FAILED',
          error: error.message
        });
      }
    }

    const incrementKey = MockHsmService.generateIncrementKey({
      merchantId,
      newBalance: merchantAccount.balance,
      timestamp: Date.now()
    });

    return {
      batchId,
      processed: results.length,
      successful: results.filter(r => r.status === 'SETTLED').length,
      failed: results.filter(r => r.status === 'FAILED').length,
      totalAmount,
      totalCommission,
      incrementKey: incrementKey.incrementKey,
      transactions: results
    };
  }

  validateBatchSeal(transactions, seal) {
    const calculatedSeal = generateBatchSeal(
      (transactions || []).map(t => ({ id: t.id, amount: t.amount }))
    );
    return seal === calculatedSeal;
  }

  calculateCommissions(amount) {
    const protocolCommission = amount * this.commissionRates.protocol;
    const bankCommission = amount * this.commissionRates.bank;
    const netAmount = amount - protocolCommission - bankCommission;
    return { protocolCommission, bankCommission, netAmount };
  }
}

module.exports = new SettlementService();
