package com.Mari.mobileapp.domain.model

enum class TransactionStatus {
    PENDING, COMPLETED, FAILED
}

enum class TransactionType {
    SEND, RECEIVE
}

enum class TransportMethod {
    SMS, RECEIVED
}

data class Transaction(
    val id: String,
    val senderBioHash: String,
    val receiverBioHash: String,
    val amount: Double,
    val locationGrid: String,
    val timestamp: Long,
    val status: TransactionStatus,
    val type: TransactionType,
    val coupon: String,
    val transportMethod: TransportMethod
)
