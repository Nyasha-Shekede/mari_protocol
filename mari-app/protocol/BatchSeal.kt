package com.Mari.mobileapp.protocol

import java.security.MessageDigest

object BatchSeal {
    /**
     * Generate batch seal compatible with @Mari/shared-libs
     * Concatenate sorted (id + amountString), where amountString is without decimals for whole numbers.
     * Then SHA-256 hex of the concatenated string.
     */
    fun generate(items: List<Pair<String, Double>>): String {
        val parts = items.map { (id, amt) ->
            val amtStr = if (amt == Math.floor(amt)) {
                amt.toLong().toString()
            } else {
                amt.toString()
            }
            id + amtStr
        }.sorted()
        val concat = parts.joinToString("")
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(concat.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { b -> "%02x".format(b) }
    }
}
