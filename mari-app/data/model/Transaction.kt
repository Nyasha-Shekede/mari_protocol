package com.Mari.mobileapp.data.model

enum class TransactionStatus { PENDING, COMPLETED, FAILED }
enum class TransactionType { SEND, RECEIVE }
enum class TransportMethod { SMS, RECEIVED }

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
    val transportMethod: TransportMethod,
    val couponHash: String? = null
) {
    companion object {
        private fun sha256Hex(s: String): String {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
            val sb = StringBuilder()
            for (b in bytes) {
                sb.append(String.format("%02x", b))
            }
            return sb.toString()
        }
        fun fromEntity(entity: com.Mari.mobileapp.data.local.entity.TransactionEntity): Transaction =
            Transaction(
                id = entity.id,
                senderBioHash = entity.senderBioHash,
                receiverBioHash = entity.receiverBioHash,
                amount = entity.amount,
                locationGrid = entity.locationGrid,
                timestamp = entity.timestamp,
                status = TransactionStatus.valueOf(entity.status),
                type = TransactionType.valueOf(entity.type),
                coupon = entity.coupon,
                transportMethod = TransportMethod.valueOf(entity.transportMethod),
                couponHash = entity.couponHash
            )

        fun toEntity(transaction: Transaction): com.Mari.mobileapp.data.local.entity.TransactionEntity =
            com.Mari.mobileapp.data.local.entity.TransactionEntity(
                id = transaction.id,
                senderBioHash = transaction.senderBioHash,
                receiverBioHash = transaction.receiverBioHash,
                amount = transaction.amount,
                locationGrid = transaction.locationGrid,
                timestamp = transaction.timestamp,
                status = transaction.status.name,
                type = transaction.type.name,
                coupon = transaction.coupon,
                transportMethod = transaction.transportMethod.name,
                couponHash = transaction.couponHash ?: sha256Hex(transaction.coupon)
            )
    }
}
