package com.Mari.mobileapp.core.agent

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Parcelable
import android.util.Log

/**
 * OFFICIAL DIRECTIVE: Enable app-to-app communication on same device
 * ALLOWS other mobile apps to request payments from Mari
 */
class LocalAgentManager(private val context: Context) {

    private var isRegistered = false

    // OFFICIAL: Register to receive payment requests from other apps
    fun registerLocalAgentHandler() {
        if (isRegistered) return
        val intentFilter = IntentFilter().apply {
            addAction(ACTION_REQUEST_PAYMENT)
            addAction(ACTION_QUERY_STATUS)
        }
        context.registerReceiver(localAgentReceiver, intentFilter)
        isRegistered = true
    }

    fun unregisterLocalAgentHandler() {
        if (!isRegistered) return
        try {
            context.unregisterReceiver(localAgentReceiver)
        } catch (_: Exception) {
            // ignore
        }
        isRegistered = false
    }

    private val localAgentReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context, intent: Intent) {
            when (intent.action) {
                ACTION_REQUEST_PAYMENT -> handlePaymentRequest(intent)
                ACTION_QUERY_STATUS -> handleStatusQuery(intent)
            }
        }
    }

    private fun handlePaymentRequest(intent: Intent) {
        // OFFICIAL: Extract request parameters
        val agentPackage = intent.getStringExtra(EXTRA_AGENT_PACKAGE)
        val constraintsJson = intent.getStringExtra(EXTRA_CONSTRAINTS)
        val callbackIntent = getParcelableExtraCompat(intent, EXTRA_CALLBACK_INTENT, Intent::class.java)

        // OFFICIAL: Security validation (stubbed; replace with real trust checks)
        if (!isTrustedLocalAgent(agentPackage)) {
            sendRejection(callbackIntent, "Untrusted agent")
            return
        }

        if (constraintsJson.isNullOrBlank()) {
            sendRejection(callbackIntent, "Missing constraints")
            return
        }

        // Launch approval activity to present UI and return result via callback intent
        try {
            // Avoid compile-time dependency on app UI module; address by class name
            val approval = Intent().apply {
                setClassName(context.packageName, "com.Mari.mobile.ui.activity.AgentApprovalActivity")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(EXTRA_AGENT_PACKAGE, agentPackage)
                putExtra(EXTRA_CONSTRAINTS, constraintsJson)
                putExtra(EXTRA_CALLBACK_INTENT, callbackIntent)
            }
            context.startActivity(approval)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start approval activity: ${e.message}")
            sendRejection(callbackIntent, "Approval UI error")
        }
    }

    private fun handleStatusQuery(intent: Intent) {
        val callbackIntent = getParcelableExtraCompat(intent, EXTRA_CALLBACK_INTENT, Intent::class.java)
        // Minimal example status response
        sendStatus(callbackIntent, status = "idle")
    }

    private fun isTrustedLocalAgent(agentPackage: String?): Boolean {
        if (agentPackage.isNullOrBlank()) return false
        // TODO: Implement proper trust list / signature verification
        return true
    }

    private fun sendRejection(callbackIntent: Intent?, reason: String) {
        if (callbackIntent == null) return
        callbackIntent.putExtra(KEY_STATUS, STATUS_REJECTED)
        callbackIntent.putExtra(KEY_REASON, reason)
        safeBroadcast(callbackIntent)
    }

    private fun sendStatus(callbackIntent: Intent?, status: String) {
        if (callbackIntent == null) return
        callbackIntent.putExtra(KEY_STATUS, status)
        safeBroadcast(callbackIntent)
    }

    private fun safeBroadcast(intent: Intent) {
        try {
            context.sendBroadcast(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send callback broadcast: ${e.message}")
        }
    }

    private fun <T : Parcelable> getParcelableExtraCompat(intent: Intent, key: String, clazz: Class<T>): T? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(key, clazz)
        } else {
            @Suppress("DEPRECATION") intent.getParcelableExtra(key) as? T
        }
    }

    companion object {
        private const val TAG = "LocalAgentManager"
        const val ACTION_REQUEST_PAYMENT = "com.Mari.agent.REQUEST_PAYMENT"
        const val ACTION_QUERY_STATUS = "com.Mari.agent.QUERY_STATUS"

        const val EXTRA_AGENT_PACKAGE = "agent_package"
        const val EXTRA_CONSTRAINTS = "constraints"
        const val EXTRA_CALLBACK_INTENT = "callback_intent"

        const val KEY_STATUS = "status"
        const val KEY_REASON = "reason"

        const val STATUS_REJECTED = "rejected"
    }
}
