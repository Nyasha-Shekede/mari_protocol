package com.Mari.mobileapp.data.repository

import com.Mari.mobileapp.data.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    suspend fun createTransaction(transaction: Transaction)
    suspend fun getTransaction(transactionId: String): Transaction?
    fun getTransactionsForUser(bioHash: String): Flow<List<Transaction>>
    suspend fun getPendingTransactions(): List<Transaction>
    suspend fun getPendingSmsTransactions(): List<Transaction>
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transactionId: String)
    suspend fun getTotalReceived(bioHash: String): Double
    suspend fun getTotalSent(bioHash: String): Double
}
