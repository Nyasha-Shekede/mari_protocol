package com.Mari.mobileapp.domain.usecase

import com.Mari.mobileapp.core.transaction.TransactionManager
import com.Mari.mobileapp.domain.model.Transaction
import com.Mari.mobileapp.domain.model.TransactionStatus
import com.Mari.mobileapp.domain.model.TransactionType
import com.Mari.mobileapp.domain.model.TransportMethod
import com.Mari.mobileapp.domain.repository.TransactionRepository
import com.Mari.mobileapp.domain.repository.UserRepository
import java.util.UUID
import javax.inject.Inject

class SendPaymentUseCase @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(recipientBio: String, amount: Double, userBioHash: String) {
        val result = transactionManager.sendPayment(recipientBio, amount)

        when (result) {
            is TransactionManager.TransactionResult.Success -> {
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    senderBioHash = userBioHash,
                    receiverBioHash = recipientBio,
                    amount = amount,
                    locationGrid = "",
                    timestamp = System.currentTimeMillis(),
                    status = TransactionStatus.PENDING,
                    type = TransactionType.SEND,
                    coupon = result.coupon,
                    transportMethod = when (result.method) {
                        TransactionManager.TransportMethod.SMS -> TransportMethod.SMS
                        TransactionManager.TransportMethod.RECEIVED -> TransportMethod.RECEIVED
                        TransactionManager.TransportMethod.INTERNET -> TransportMethod.SMS // treat as SMS transport for domain persistence
                    }
                )

                transactionRepository.createTransaction(transaction)

                val user = userRepository.getUserByBloodHash(userBioHash)
                user?.let {
                    val newReadyCash = it.readyCash - amount
                    val newTotalMoney = it.totalMoney
                    userRepository.updateBalance(it.id, newReadyCash, newTotalMoney)
                }
            }
            is TransactionManager.TransactionResult.Queued -> {
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    senderBioHash = userBioHash,
                    receiverBioHash = recipientBio,
                    amount = amount,
                    locationGrid = "",
                    timestamp = System.currentTimeMillis(),
                    status = TransactionStatus.PENDING,
                    type = TransactionType.SEND,
                    coupon = result.coupon,
                    transportMethod = TransportMethod.SMS
                )

                transactionRepository.createTransaction(transaction)

                val user = userRepository.getUserByBloodHash(userBioHash)
                user?.let {
                    val newReadyCash = it.readyCash - amount
                    val newTotalMoney = it.totalMoney
                    userRepository.updateBalance(it.id, newReadyCash, newTotalMoney)
                }
            }
            is TransactionManager.TransactionResult.Error -> {
                throw Exception(result.message)
            }
        }
    }
}
