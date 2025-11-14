package com.Mari.mobileapp.domain.util

import com.Mari.mobileapp.domain.model.Transaction
import java.util.concurrent.TimeUnit

object TransactionValidator {
    private const val MAX_AMOUNT = 10000.0
    private const val MIN_AMOUNT = 0.01
    private const val EXPIRY_HOURS = 24L

    fun validateTransaction(transaction: Transaction): ValidationResult {
        return when {
            transaction.amount <= 0 -> ValidationResult.Invalid("Amount must be positive")
            transaction.amount > MAX_AMOUNT -> ValidationResult.Invalid("Amount exceeds maximum limit")
            transaction.amount < MIN_AMOUNT -> ValidationResult.Invalid("Amount below minimum threshold")
            isExpired(transaction.timestamp) -> ValidationResult.Invalid("Transaction expired")
            transaction.senderBioHash.isBlank() -> ValidationResult.Invalid("Invalid sender")
            transaction.receiverBioHash.isBlank() -> ValidationResult.Invalid("Invalid receiver")
            else -> ValidationResult.Valid
        }
    }

    private fun isExpired(timestamp: Long): Boolean {
        val age = System.currentTimeMillis() - timestamp
        val ageHours = TimeUnit.MILLISECONDS.toHours(age)
        return ageHours > EXPIRY_HOURS
    }
}

sealed class ValidationResult {
    object Valid : ValidationResult()
    data class Invalid(val reason: String) : ValidationResult()
}
