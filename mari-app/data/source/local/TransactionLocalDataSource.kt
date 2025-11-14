package com.Mari.mobileapp.data.source.local

import com.Mari.mobileapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionLocalDataSource {
    suspend fun saveTransaction(transaction: Transaction)
    suspend fun getTransaction(transactionId: String): Transaction?
    fun getTransactionsForUser(bioHash: String): Flow<List<Transaction>>
    suspend fun getPendingTransactions(): List<Transaction>
    suspend fun getPendingSmsTransactions(): List<Transaction>
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)
}
