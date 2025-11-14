const SettlementService = require('../services/settlementService');

class SettlementController {
  async processSettlement(req, res) {
    try {
      const result = await SettlementService.processSettlementRequest(req.body);
      res.json({
        success: true,
        data: result
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }
}

module.exports = new SettlementController();
