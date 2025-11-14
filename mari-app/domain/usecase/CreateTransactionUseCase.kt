package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.model.Transaction
import com.Mari.mobileapp.domain.repository.TransactionRepository
import javax.inject.Inject

class CreateTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(transaction: Transaction) {
        transactionRepository.createTransaction(transaction)
    }
}
