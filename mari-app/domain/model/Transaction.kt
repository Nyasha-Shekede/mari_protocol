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

/**
 * Transaction domain model
 * 
 * IMPORTANT: "senderBioHash" and "receiverBioHash" are TERRIBLE variable names!
 * They are NOT biometric data - they're just pseudonymous user account identifiers.
 * Think of them as: senderId, receiverId, senderAccountId, receiverAccountId
 * 
 * NO special biometric hardware is needed - these are just random unique IDs
 * that identify users in the system (like account numbers).
 */
data class Transaction(
    val id: String,
    
    // WARNING: Misleading names! These are NOT biometric data.
    // They're just pseudonymous user identifiers (like account IDs).
    // Should be renamed to: senderId, receiverId, etc.
    val senderBioHash: String,      // TODO: Rename to senderId
    val receiverBioHash: String,    // TODO: Rename to receiverId
    
    val amount: Double,
    val locationGrid: String,
    val timestamp: Long,
    val status: TransactionStatus,
    val type: TransactionType,
    val coupon: String,
    val transportMethod: TransportMethod
)
