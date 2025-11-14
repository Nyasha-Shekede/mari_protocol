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

class ReceivePaymentUseCase @Inject constructor(
    private val transactionManager: TransactionManager,
    private val userRepository: UserRepository,
    private val transactionRepository: TransactionRepository
) {
    suspend operator fun invoke(coupon: String, userBioHash: String) {
        val result = transactionManager.receivePayment(coupon)

        when (result) {
            is TransactionManager.TransactionResult.Success -> {
                val parsedCoupon = transactionManager.parseCoupon(coupon)

                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    senderBioHash = parsedCoupon.senderBio,
                    receiverBioHash = userBioHash,
                    amount = parsedCoupon.amount,
                    locationGrid = parsedCoupon.grid,
                    timestamp = System.currentTimeMillis(),
                    status = TransactionStatus.PENDING,
                    type = TransactionType.RECEIVE,
                    coupon = coupon,
                    transportMethod = TransportMethod.RECEIVED
                )

                transactionRepository.createTransaction(transaction)

                val user = userRepository.getUserByBloodHash(userBioHash)
                user?.let {
                    val newReadyCash = it.readyCash
                    val newTotalMoney = it.totalMoney + parsedCoupon.amount
                    userRepository.updateBalance(it.id, newReadyCash, newTotalMoney)
                }
            }
            is TransactionManager.TransactionResult.Error -> {
                throw Exception(result.message)
            }
            else -> {}
        }
    }
}
