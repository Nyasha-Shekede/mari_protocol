package com.Mari.mobileapp.data.local.dao

import androidx.room.*
import com.Mari.mobileapp.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE id = :transactionId")
    suspend fun getTransaction(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE sender_bio_hash = :bioHash OR receiver_bio_hash = :bioHash ORDER BY timestamp DESC")
    fun getTransactionsForUser(bioHash: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING'")
    suspend fun getPendingTransactions(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE status = 'PENDING' AND transport_method = 'SMS'")
    suspend fun getPendingSmsTransactions(): List<TransactionEntity>

    @Query("DELETE FROM transactions WHERE id = :transactionId")
    suspend fun deleteTransaction(transactionId: String)

    @Query("SELECT SUM(amount) FROM transactions WHERE receiver_bio_hash = :bioHash AND status = 'COMPLETED'")
    suspend fun getTotalReceived(bioHash: String): Double

    @Query("SELECT SUM(amount) FROM transactions WHERE sender_bio_hash = :bioHash AND status = 'COMPLETED'")
    suspend fun getTotalSent(bioHash: String): Double
}
