package com.Mari.mobileapp.domain.usecase

import android.content.Context
import com.Mari.mobileapp.domain.model.Transaction
import com.Mari.mobileapp.domain.model.TransactionStatus
import com.Mari.mobileapp.domain.repository.TransactionRepository
import com.Mari.mobileapp.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Applies a verified receipt (after on-device cryptographic verification).
 * Idempotent: tracks processed receipt hashes to avoid double application.
 */
class ApplyReceiptUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) {
    private val prefs by lazy { context.getSharedPreferences("Mari_receipts", Context.MODE_PRIVATE) }

    suspend operator fun invoke(
        currentUserId: String,
        role: String,
        amount: Double,
        shortHash: String
    ): Boolean {
        // Only apply to payee wallet
        if (role.lowercase() != "payee") return false

        val key = "$currentUserId:$shortHash:$amount:$role"
        if (prefs.getBoolean(key, false)) {
            // already applied
            return false
        }

        // Prefer matching by coupon short hash when available; fallback to amount-only heuristic
        val pending = transactionRepository.getPendingTransactions()
        val candidateByHash = pending.firstOrNull {
            it.receiverBioHash == currentUserId && sha256Hex(it.coupon).startsWith(shortHash, ignoreCase = true)
        }
        val candidate = candidateByHash ?: pending.firstOrNull {
            it.receiverBioHash == currentUserId && kotlin.math.abs(it.amount - amount) < 1e-9
        }

        if (candidate != null) {
            // Mark transaction completed
            val updated = candidate.copy(status = TransactionStatus.COMPLETED)
            transactionRepository.updateTransaction(updated)

            // Update user balance: add amount to totalMoney; readyCash stays same in this model
            val user = userRepository.getUserByBloodHash(currentUserId)
            user?.let {
                val newReady = it.readyCash
                val newTotal = it.totalMoney + amount
                userRepository.updateBalance(it.id, newReady, newTotal)
            }

            // Mark idempotent key as applied
            prefs.edit().putBoolean(key, true).apply()
            return true
        }
        return false
    }
    private fun sha256Hex(s: String): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(s.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder()
        for (b in bytes) sb.append(String.format("%02x", b))
        return sb.toString()
    }
}
