package com.Mari.mobileapp.domain.util

import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object CouponGenerator {
    private const val COUPON_PREFIX = "AUR"
    private const val SEPARATOR = "|"
    private const val ALGORITHM = "HmacSHA256"

    fun generateCoupon(
        senderBio: String,
        receiverBio: String,
        amount: Double,
        grid: String,
        secretKey: String
    ): String {
        val timestamp = System.currentTimeMillis()
        val payload = listOf(senderBio, receiverBio, amount, grid, timestamp).joinToString(SEPARATOR)
        val signature = sign(payload, secretKey)
        val encoded = Base64.getUrlEncoder().encodeToString("$payload$SEPARATOR$signature".toByteArray())
        return "$COUPON_PREFIX$encoded"
    }

    fun verifyCoupon(coupon: String, secretKey: String): Boolean {
        return try {
            val raw = coupon.removePrefix(COUPON_PREFIX)
            val decoded = String(Base64.getUrlDecoder().decode(raw))
            val parts = decoded.split(SEPARATOR)
            if (parts.size != 6) return false
            val payload = parts.take(5).joinToString(SEPARATOR)
            val signature = parts[5]
            val expectedSignature = sign(payload, secretKey)
            signature == expectedSignature
        } catch (e: Exception) {
            false
        }
    }

    private fun sign(data: String, secret: String): String {
        val mac = Mac.getInstance(ALGORITHM)
        val secretKey = SecretKeySpec(secret.toByteArray(), ALGORITHM)
        mac.init(secretKey)
        return Base64.getUrlEncoder().encodeToString(mac.doFinal(data.toByteArray()))
    }
}
