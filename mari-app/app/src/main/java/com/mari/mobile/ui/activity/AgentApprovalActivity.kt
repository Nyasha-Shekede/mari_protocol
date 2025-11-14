package com.Mari.mobile.ui.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.Mari.mobile.ui.agent.LocalAgentApprovalOverlay
import com.Mari.mobileapp.core.agent.PaymentConstraints
import com.Mari.mobileapp.core.security.DeviceKeyManager

/**
 * Presents an approval overlay for local agent payment requests and returns result via callback intent.
 */
class AgentApprovalActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val agentPkg = intent.getStringExtra(EXTRA_AGENT_PACKAGE) ?: "unknown-agent"
        val constraintsJson = intent.getStringExtra(EXTRA_CONSTRAINTS) ?: "{}"
        val callbackIntent = intent.getParcelableExtra<Intent>(EXTRA_CALLBACK_INTENT)
        val deepLinkSessionId = parseDeepLinkSessionId(intent)
        val initialConstraints = parseConstraints(constraintsJson)

        setContent {
            val dismissed = remember { mutableStateOf(false) }
            val constraintsState = remember { mutableStateOf(initialConstraints) }
            // If launched via deep link, fetch session to populate constraints for display
            if (deepLinkSessionId != null) {
                // Fire-and-forget fetch; update state when done
                Thread {
                    val fetched = fetchSessionConstraints(deepLinkSessionId)
                    if (fetched != null) {
                        runOnUiThread { constraintsState.value = fetched }
                    }
                }.start()
            }
            if (!dismissed.value) {
                LocalAgentApprovalOverlay(
                    agentDisplayName = agentPkg,
                    trustLevel = computeTrustLevel(agentPkg),
                    constraints = constraintsState.value,
                    onApproved = {
                        dismissed.value = true
                        if (deepLinkSessionId != null && callbackIntent == null) {
                            // Deep link approval: notify server directly
                            approveServerSession(deepLinkSessionId)
                        } else {
                            sendApprovalResult(callbackIntent, status = "approved", signedCoupon = generateAgentCoupon(constraintsState.value))
                        }
                        finish()
                    },
                    onRejected = {
                        dismissed.value = true
                        sendApprovalResult(callbackIntent, status = "rejected", signedCoupon = null)
                        finish()
                    }
                )
            }
        }
    }

    private fun parseConstraints(json: String): PaymentConstraints {
        return try {
            val obj = org.json.JSONObject(json)
            val max = obj.optLong("max_amount", obj.optLong("maxAmount", 0L))
            val merchantsJson = obj.optJSONArray("merchant_whitelist") ?: obj.optJSONArray("merchantWhitelist")
            val merchants = mutableListOf<String>()
            if (merchantsJson != null) {
                for (i in 0 until merchantsJson.length()) {
                    merchants.add(merchantsJson.optString(i))
                }
            }
            val category = obj.optString("category", null)
            val description = obj.optString("description", null)
            PaymentConstraints(maxAmount = max, merchantWhitelist = merchants, category = category, description = description)
        } catch (_: Exception) {
            PaymentConstraints(maxAmount = 0L)
        }
    }

    private fun computeTrustLevel(packageName: String): String {
        // Trust level computation based on package signature and whitelist
        // In production, verify package signature against known trusted agents
        return when {
            packageName.startsWith("com.Mari.") -> "verified"
            packageName.contains("agent") -> "trusted"
            else -> "unknown"
        }
    }
    
    private fun generateAgentCoupon(constraints: PaymentConstraints): String {
        // Agent coupon with constraints attached
        val expiry = System.currentTimeMillis() + 5 * 60 * 1000
        val seal = generateSeal()
        val constraintsJson = org.json.JSONObject().apply {
            put("max_amount", constraints.maxAmount)
            put("merchant_whitelist", org.json.JSONArray(constraints.merchantWhitelist))
            if (!constraints.category.isNullOrBlank()) put("category", constraints.category)
            if (!constraints.description.isNullOrBlank()) put("humanBlurb", constraints.description)
            put("version", "2.0")
            put("createdAt", System.currentTimeMillis())
        }.toString()
        val condB64 = android.util.Base64.encodeToString(constraintsJson.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        val category = constraints.category ?: ""
        val catParam = if (category.isNotBlank()) "&cat=${'$'}{encode(category)}" else ""
        val base = "Mari://xfer?from=local-agent&to=agent-proxy&val=0&g=global&exp=${'$'}expiry&s=${'$'}seal${'$'}catParam&cond=${'$'}{encode(condB64)}"
        // Try device signing (ECDSA P-256 via Android Keystore) over coupon base only.
        // Server verifies over base; constraints are bound via COND_HASH at HSM.
        val sigB64Url = try {
            val canonical = base.toByteArray(Charsets.UTF_8)
            DeviceKeyManager.signToBase64Url(canonical)
        } catch (_: Exception) {
            demoSignature(base, constraintsJson)
        }
        val kid = try { DeviceKeyManager.getKid() } catch (_: Exception) { null }
        val kidParam = if (!kid.isNullOrBlank()) "&kid=${'$'}kid" else ""
        return "${'$'}base&sig=${'$'}{encode(sigB64Url)}${'$'}kidParam"
    }

    private fun demoSignature(coupon: String, constraintsJson: String): String {
        // DEMO-ONLY: Not a cryptographic device signature. Replace with ECDSA P-256 when key manager is available.
        val md = java.security.MessageDigest.getInstance("SHA-256")
        md.update(coupon.toByteArray(Charsets.UTF_8))
        md.update(0x2E) // delimiter
        md.update(constraintsJson.toByteArray(Charsets.UTF_8))
        val hash = md.digest()
        return android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP)
    }

    private fun encode(s: String): String = java.net.URLEncoder.encode(s, "UTF-8")

    private fun generateSeal(): String {
        // Generate cryptographic seal using SecureRandom
        val rnd = java.security.SecureRandom()
        val bytes = ByteArray(16) // 128-bit seal
        rnd.nextBytes(bytes)
        return bytes.joinToString("") { String.format("%02x", it) }
    }

    private fun sendApprovalResult(callbackIntent: Intent?, status: String, signedCoupon: String?) {
        if (callbackIntent == null) return
        try {
            callbackIntent.putExtra(KEY_STATUS, status)
            if (signedCoupon != null) callbackIntent.putExtra(KEY_SIGNED_COUPON, signedCoupon)
            sendBroadcast(callbackIntent)
            setResult(Activity.RESULT_OK)
        } catch (_: Exception) {
            setResult(Activity.RESULT_CANCELED)
        }
    }

    private fun parseDeepLinkSessionId(intent: Intent?): String? {
        val data = intent?.data ?: return null
        if ("Mari" != data.scheme || "approve" != data.host) return null
        return data.getQueryParameter("session")
    }

    private fun approveServerSession(sessionId: String) {
        Thread {
            try {
                val url = java.net.URL("${'$'}{apiBaseUrl()}/v2/approvals/${'$'}sessionId/approve")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                }
                val body = "{\"user_id\":\"dl-user\",\"device_id\":\"android\"}"
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                conn.inputStream.use { it.readBytes() }
                conn.disconnect()
            } catch (_: Exception) { }
        }.start()
    }

    private fun fetchSessionConstraints(sessionId: String): PaymentConstraints? {
        return try {
            val url = java.net.URL("${'$'}{apiBaseUrl()}/v2/sessions/${'$'}sessionId")
            val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                requestMethod = "GET"
            }
            val body = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            conn.disconnect()
            val obj = org.json.JSONObject(body)
            val constraints = obj.optJSONObject("constraints")
            if (constraints != null) parseConstraints(constraints.toString()) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun apiBaseUrl(): String {
        // Emulator default maps localhost to 10.0.2.2
        val env = System.getProperty("Mari_API_BASE_URL") ?: System.getenv("Mari_API_BASE_URL")
        return (env ?: "http://10.0.2.2:3000").trimEnd('/')
    }


    companion object {
        const val EXTRA_AGENT_PACKAGE = "agent_package"
        const val EXTRA_CONSTRAINTS = "constraints"
        const val EXTRA_CALLBACK_INTENT = "callback_intent"

        const val KEY_STATUS = "status"
        const val KEY_SIGNED_COUPON = "signed_coupon"
    }
}
