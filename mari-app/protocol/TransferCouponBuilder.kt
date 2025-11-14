package com.Mari.mobile.protocol

import java.security.MessageDigest
import kotlin.math.abs

object TransferCouponBuilder {
    // Motion seal algorithm must match server's demo implementation:
    // md5 of "x,y,z" -> take first 8 hex chars, parse as int base16, divide by 100000000 to Double
    private fun motionSeal(x: Double, y: Double, z: Double): Double {
        val motionString = "$x,$y,$z"
        val md5 = MessageDigest.getInstance("MD5")
        val digest = md5.digest(motionString.toByteArray())
        val hex = digest.joinToString("") { b -> "%02x".format(b) }
        val first8 = hex.substring(0, 8)
        val intVal = first8.toLong(16)
        return intVal.toDouble() / 100_000_000.0
    }

    // Generate coupon seal 's' used by server as motion seal in demo
    fun computeSealFromMotion(x: Double, y: Double, z: Double): String {
        return motionSeal(x, y, z).toString()
    }

    // Build Mari transfer coupon string expected by server's MariStringParser:
    // Mari://xfer?from=<senderBio>&to=<receiverBio>&val=<amount>&g=<grid>&exp=<epoch_ms>&s=<seal>
    fun buildTransferCoupon(
        senderBio: String,
        receiverBio: String,
        amount: Double,
        grid: String,
        expiryEpochMs: Long,
        seal: String
    ): String {
        val amt = if (amount % 1.0 == 0.0) amount.toInt().toString() else amount.toString()
        return "Mari://xfer?from=${senderBio}&to=${receiverBio}&val=${amt}&g=${grid}&exp=${expiryEpochMs}&s=${seal}"
    }
}
