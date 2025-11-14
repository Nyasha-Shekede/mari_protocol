package com.Mari.mobileapp.core.transaction

import com.Mari.mobileapp.domain.util.Result

interface TransactionManager {
    enum class TransportMethod {
        SMS,
        INTERNET,
        RECEIVED
    }

    sealed class TransactionResult {
        data class Success(val coupon: String, val method: TransportMethod) : TransactionResult()
        data class Queued(val coupon: String) : TransactionResult()
        data class Error(val message: String) : TransactionResult()
    }

    data class ParsedCoupon(
        val senderBio: String,
        val receiverBio: String,
        val amount: Double,
        val grid: String,
        val timestamp: Long,
        val signature: String
    )

    suspend fun sendPayment(recipientBio: String, amount: Double): TransactionResult
    suspend fun receivePayment(coupon: String): TransactionResult
    fun parseCoupon(coupon: String): ParsedCoupon
    suspend fun trySendViaSms(recipient: String, coupon: String): Boolean
}
