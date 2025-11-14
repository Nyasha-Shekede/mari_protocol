const BankAccountService = require('../services/bankAccountService');

class AccountController {
  async createAccount(req, res) {
    try {
      const account = BankAccountService.createAccount(req.body);
      res.status(201).json({
        success: true,
        data: account
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }

  async getAccount(req, res) {
    try {
      const account = BankAccountService.getAccount(req.params.id);
      if (!account) {
        return res.status(404).json({
          success: false,
          error: 'Account not found'
        });
      }

      res.json({
        success: true,
        data: account
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }

  async createReserve(req, res) {
    try {
      const reserve = BankAccountService.createReserveAccount(req.body);
      res.status(201).json({
        success: true,
        data: reserve
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }

  async getReserve(req, res) {
    try {
      const reserve = BankAccountService.getReserveByBioHash(req.params.bioHash);
      if (!reserve) {
        return res.status(404).json({
          success: false,
          error: 'Reserve account not found'
        });
      }

      res.json({
        success: true,
        data: reserve
      });
    } catch (error) {
      res.status(400).json({
        success: false,
        error: error.message
      });
    }
  }
}

module.exports = new AccountController();
