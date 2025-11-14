package com.Mari.mobileapp.data.source.local.impl

import com.Mari.mobileapp.data.local.dao.TransactionDao
import com.Mari.mobileapp.data.model.Transaction
import com.Mari.mobileapp.data.source.local.TransactionLocalDataSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionLocalDataSourceImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionLocalDataSource {

    override suspend fun saveTransaction(transaction: Transaction) =
        transactionDao.insertTransaction(Transaction.toEntity(transaction))

    override suspend fun getTransaction(transactionId: String): Transaction? =
        transactionDao.getTransaction(transactionId)?.let { Transaction.fromEntity(it) }

    override fun getTransactionsForUser(bioHash: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsForUser(bioHash)
            .map { list -> list.map { Transaction.fromEntity(it) } }

    override suspend fun getPendingTransactions(): List<Transaction> =
        transactionDao.getPendingTransactions().map { Transaction.fromEntity(it) }

    override suspend fun getPendingSmsTransactions(): List<Transaction> =
        transactionDao.getPendingSmsTransactions().map { Transaction.fromEntity(it) }

    override suspend fun updateTransaction(transaction: Transaction) =
        transactionDao.updateTransaction(Transaction.toEntity(transaction))

    override suspend fun deleteTransaction(transactionId: String) =
        transactionDao.deleteTransaction(transactionId)
}
