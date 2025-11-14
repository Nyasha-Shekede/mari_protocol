package com.Mari.mobileapp.core.crypto

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyFactory
import java.security.PublicKey
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * BankVerifier performs offline verification of increment key proofs using the bank's public key.
 * First-run TOFU: if no cached public key is present, it fetches from BANK_BASE_URL /hsm/public-key,
 * caches it locally, and uses it for subsequent offline verification.
 */
class BankVerifier(private val context: Context) {
    private val prefs by lazy { context.getSharedPreferences("bank_verifier", Context.MODE_PRIVATE) }

    suspend fun verifyOrFetch(payloadCanonicalJson: String, sigB64: String): Boolean {
        val pub = getOrFetchPublicKey() ?: return false
        return verifyWith(pub, payloadCanonicalJson, sigB64)
    }

    private fun extractStringField(obj: String, name: String): String? {
        val marker = "\"$name\":\""
        val idx = obj.indexOf(marker)
        if (idx < 0) return null
        val start = idx + marker.length
        val end = obj.indexOf('"', start)
        if (end <= start) return null
        return obj.substring(start, end)
    }

    private fun extractNumberField(obj: String, name: String): String? {
        val marker = "\"$name\":"
        val idx = obj.indexOf(marker)
        if (idx < 0) return null
        var j = idx + marker.length
        val sb = StringBuilder()
        while (j < obj.length) {
            val c = obj[j]
            if ((c in '0'..'9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') sb.append(c) else break
            j++
        }
        return if (sb.isEmpty()) null else sb.toString()
    }

    private fun verifyWith(pub: PublicKey, canonicalJson: String, sigB64: String): Boolean {
        return try {
            val sig = Signature.getInstance("SHA256withRSA")
            sig.initVerify(pub)
            sig.update(canonicalJson.toByteArray(Charsets.UTF_8))
            sig.verify(Base64.decode(sigB64, Base64.DEFAULT))
        } catch (_: Exception) { false }
    }

    data class VerifiedProof(
        val userId: String,
        val amount: Double,
        val couponHash: String,
        val timeNs: Long,
        val version: Int,
        val hsmKid: String
    )

    suspend fun verifyProofFromJson(proofJson: String): VerifiedProof? {
        return try {
            // Expect { "payload": { ... }, "SIG": "base64" }
            val payloadStart = proofJson.indexOf("\"payload\"")
            val sigStart = proofJson.indexOf("\"SIG\"")
            if (payloadStart < 0 || sigStart < 0) return null
            // Very small JSON parser to avoid dependencies: find payload object braces
            val objStart = proofJson.indexOf('{', payloadStart)
            var depth = 0
            var i = objStart
            var objEnd = -1
            while (i < proofJson.length) {
                val c = proofJson[i]
                if (c == '{') depth++
                if (c == '}') { depth--; if (depth == 0) { objEnd = i; break } }
                i++
            }
            if (objStart < 0 || objEnd < 0) return null
            val payloadObj = proofJson.substring(objStart, objEnd + 1)
            // Extract SIG (assumes simple top-level string field)
            val sigMarker = "\"SIG\":\""
            val sidx = proofJson.indexOf(sigMarker)
            if (sidx < 0) return null
            val sStart = sidx + sigMarker.length
            val sEnd = proofJson.indexOf('"', sStart)
            if (sEnd <= sStart) return null
            val sigB64 = proofJson.substring(sStart, sEnd)

            val canonical = canonicalizePayload(payloadObj)
            val pub = getOrFetchPublicKey() ?: return null
            val ok = verifyWith(pub, canonical, sigB64)
            if (!ok) return null
            // Extract fields for downstream logic
            val userId = extractStringField(payloadObj, "USER_ID") ?: return null
            val amount = extractNumberField(payloadObj, "AMOUNT")?.toDoubleOrNull() ?: return null
            val couponHash = extractStringField(payloadObj, "COUPON_HASH") ?: return null
            val timeNs = extractNumberField(payloadObj, "TIME_NS")?.toLongOrNull() ?: 0L
            val version = extractNumberField(payloadObj, "VERSION")?.toIntOrNull() ?: 0
            val kid = extractStringField(payloadObj, "HSM_KID") ?: ""
            VerifiedProof(userId = userId, amount = amount, couponHash = couponHash, timeNs = timeNs, version = version, hsmKid = kid)
        } catch (_: Exception) { null }
    }

    // Build canonical JSON string with lex-sorted keys and no spaces from a payload object string
    private fun canonicalizePayload(payloadJsonObj: String): String {
        // Very small parser tailored to known fields (AMOUNT number, COUPON_HASH string, HSM_KID string, TIME_NS number, USER_ID string, VERSION number)
        fun extractString(name: String): String? {
            val marker = "\"$name\":\""
            val idx = payloadJsonObj.indexOf(marker)
            if (idx < 0) return null
            val start = idx + marker.length
            val end = payloadJsonObj.indexOf('"', start)
            if (end <= start) return null
            return payloadJsonObj.substring(start, end)
        }
        fun extractNumber(name: String): String? {
            val marker = "\"$name\":"
            val idx = payloadJsonObj.indexOf(marker)
            if (idx < 0) return null
            var j = idx + marker.length
            val sb = StringBuilder()
            while (j < payloadJsonObj.length) {
                val c = payloadJsonObj[j]
                if ((c >= '0' && c <= '9') || c == '.' || c == 'e' || c == 'E' || c == '+' || c == '-') sb.append(c) else break
                j++
            }
            return if (sb.isEmpty()) null else sb.toString()
        }

        val map = sortedMapOf<String, String>()
        extractNumber("AMOUNT")?.let { map["AMOUNT"] = it }
        extractString("COUPON_HASH")?.let { map["COUPON_HASH"] = "\"$it\"" }
        extractString("HSM_KID")?.let { map["HSM_KID"] = "\"$it\"" }
        extractNumber("TIME_NS")?.let { map["TIME_NS"] = it }
        extractString("USER_ID")?.let { map["USER_ID"] = "\"$it\"" }
        extractNumber("VERSION")?.let { map["VERSION"] = it }

        return buildString {
            append('{')
            var first = true
            for ((k, v) in map) {
                if (!first) append(',')
                append('"').append(k).append('"').append(':').append(v)
                first = false
            }
            append('}')
        }
    }

    private suspend fun getOrFetchPublicKey(): PublicKey? {
        val cached = prefs.getString(KEY_PUBKEY_PEM, null)
        if (cached != null) return pemToPublicKey(cached)
        val pem = fetchPublicKeyPem() ?: return null
        prefs.edit().putString(KEY_PUBKEY_PEM, pem).apply()
        return pemToPublicKey(pem)
    }

    private fun pemToPublicKey(pem: String): PublicKey? {
        return try {
            val clean = pem
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\n", "")
                .replace("\r", "")
            val der = Base64.decode(clean, Base64.DEFAULT)
            val spec = X509EncodedKeySpec(der)
            val kf = KeyFactory.getInstance("RSA")
            kf.generatePublic(spec)
        } catch (_: Exception) { null }
    }

    private suspend fun fetchPublicKeyPem(): String? = withContext(Dispatchers.IO) {
        try {
            val base = com.Mari.mobile.BuildConfig.BANK_BASE_URL
            val url = URL("$base/api/hsm/public-key")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 2000
            conn.readTimeout = 2000
            conn.setRequestProperty("Accept", "application/json")
            conn.inputStream.use { ins ->
                val body = ins.bufferedReader().readText()
                // Expect { success: true, data: { publicKeyPem: "...", keyId: "..." } }
                val marker = "publicKeyPem\":\""
                val idx = body.indexOf(marker)
                if (idx < 0) return@withContext null
                val start = idx + marker.length
                val end = body.indexOf("\"", start)
                if (end <= start) return@withContext null
                val pemEscaped = body.substring(start, end)
                return@withContext pemEscaped
                    .replace("\\n", "\n")
                    .replace("\\r", "\r")
            }
        } catch (_: Exception) { null }
    }

    companion object {
        private const val KEY_PUBKEY_PEM = "bank_pubkey_pem"
    }
}
