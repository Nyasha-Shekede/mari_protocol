package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.core.transaction.TransactionManager
import com.Mari.mobileapp.domain.repository.TransactionRepository
import javax.inject.Inject

class ProcessPendingTransactionsUseCase @Inject constructor(
    private val transactionManager: TransactionManager,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke() {
        val pendingTransactions = transactionRepository.getPendingSmsTransactions()

        for (transaction in pendingTransactions) {
            val success = transactionManager.trySendViaSms("", transaction.coupon)

            if (success) {
                transactionRepository.deleteTransaction(transaction.id)
            }
        }
    }
}
