const { v4: uuidv4 } = require('uuid');

class BankAccountService {
  constructor() {
    this.accounts = new Map();
    this.transactions = new Map();
    this.reserves = new Map();
  }

  createAccount(accountData) {
    const accountId = uuidv4();
    const account = {
      id: accountId,
      ...accountData,
      balance: accountData.initialBalance || 0,
      createdAt: new Date(),
      updatedAt: new Date(),
      isActive: true
    };

    this.accounts.set(accountId, account);
    return account;
  }

  getAccount(accountId) {
    return this.accounts.get(accountId);
  }

  getAccountByBioHash(bioHash) {
    for (const account of this.accounts.values()) {
      if (account.mariBioHash === bioHash) {
        return account;
      }
    }
    return null;
  }

  updateBalance(accountId, amount) {
    const account = this.accounts.get(accountId);
    if (!account) {
      throw new Error('Account not found');
    }

    account.balance += amount;
    account.updatedAt = new Date();
    this.accounts.set(accountId, account);

    return account;
  }

  createReserveAccount(userData) {
    const reserveId = uuidv4();
    const reserveAccount = {
      id: reserveId,
      mariBioHash: userData.bioHash,
      balance: userData.initialReserve || 0,
      userId: userData.userId,
      createdAt: new Date(),
      updatedAt: new Date()
    };

    this.reserves.set(reserveId, reserveAccount);
    return reserveAccount;
  }

  getReserveByBioHash(bioHash) {
    for (const reserve of this.reserves.values()) {
      if (reserve.mariBioHash === bioHash) {
        return reserve;
      }
    }
    return null;
  }

  updateReserveBalance(bioHash, amount) {
    const reserve = this.getReserveByBioHash(bioHash);
    if (!reserve) {
      throw new Error('Reserve account not found');
    }

    reserve.balance += amount;
    reserve.updatedAt = new Date();
    
    for (const [id, r] of this.reserves) {
      if (r.mariBioHash === bioHash) {
        this.reserves.set(id, reserve);
        break;
      }
    }

    return reserve;
  }

  processReserveTransfer(senderBioHash, receiverAccountId, amount) {
    const reserve = this.getReserveByBioHash(senderBioHash);
    if (!reserve) {
      throw new Error('Sender reserve account not found');
    }

    if (reserve.balance < amount) {
      throw new Error('Insufficient reserve balance');
    }

    const receiverAccount = this.accounts.get(receiverAccountId);
    if (!receiverAccount) {
      throw new Error('Receiver account not found');
    }

    reserve.balance -= amount;
    receiverAccount.balance += amount;

    const transactionId = uuidv4();
    const transaction = {
      id: transactionId,
      type: 'RESERVE_TRANSFER',
      fromReserve: reserve.id,
      toAccount: receiverAccountId,
      amount,
      status: 'COMPLETED',
      timestamp: new Date()
    };

    this.transactions.set(transactionId, transaction);

    return {
      transaction,
      newReserveBalance: reserve.balance,
      newAccountBalance: receiverAccount.balance
    };
  }
}

module.exports = new BankAccountService();
