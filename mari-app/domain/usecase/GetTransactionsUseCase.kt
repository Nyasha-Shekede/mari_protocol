package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.model.Transaction
import com.Mari.mobileapp.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(bioHash: String): Flow<List<Transaction>> {
        return transactionRepository.getTransactionsForUser(bioHash)
    }

    suspend fun getPendingTransactions(): List<Transaction> {
        return transactionRepository.getPendingTransactions()
    }

    suspend fun getPendingSmsTransactions(): List<Transaction> {
        return transactionRepository.getPendingSmsTransactions()
    }
}
