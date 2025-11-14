package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.domain.repository.TransactionRepository
import javax.inject.Inject

class GetTransactionSummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(bioHash: String): Triple<Double, Double, Double> {
        val totalReceived = transactionRepository.getTotalReceived(bioHash)
        val totalSent = transactionRepository.getTotalSent(bioHash)
        val net = totalReceived - totalSent
        return Triple(totalReceived, totalSent, net)
    }
}
