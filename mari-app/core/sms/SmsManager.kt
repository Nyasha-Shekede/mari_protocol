package com.Mari.mobileapp.core.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.provider.Telephony
import android.telephony.SmsMessage
import android.telephony.SubscriptionManager
import com.Mari.mobileapp.core.crypto.MariCryptoManager
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.Mari.mobile.BuildConfig
import android.telephony.SmsManager as AndroidSmsManager

/**
 * Handles SMS-based communication for Mari protocol fallback
 */
class SmsManager(
    private val context: Context,
    private val cryptoManager: MariCryptoManager
) {
    private val smsManager: AndroidSmsManager by lazy { resolveSmsManager() }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // State management
    private val _smsState = MutableStateFlow<SmsState>(SmsState.IDLE)
    val smsState: StateFlow<SmsState> = _smsState

    private val _receivedMessages = MutableStateFlow<List<SmsMessageData>>(emptyList())
    val receivedMessages: StateFlow<List<SmsMessageData>> = _receivedMessages

    private val pendingMessages = mutableListOf<SmsMessageData>()
    private val pendingProofParts = mutableMapOf<String, MutableMap<Int, String>>() // key by sender or global

    companion object {
        private const val Mari_SMS_PREFIX = "Mari_SMS:"
        private const val Mari_SMS_MAX_LENGTH = 160
        private const val Mari_SMS_MULTI_PART_PREFIX = "Mari_SMS_PART:"
        private const val Mari_RCPT_PREFIX = "Mari_RCPT:"
    }

    /**
     * SMS broadcast receiver for incoming messages
     */
    private val smsReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == "android.provider.Telephony.SMS_RECEIVED") {
                // Prefer the framework helper to extract messages
                val msgs: Array<SmsMessage>? = try {
                    Telephony.Sms.Intents.getMessagesFromIntent(intent)
                } catch (_: Exception) { null }

                if (msgs != null && msgs.isNotEmpty()) {
                    msgs.forEach { msg ->
                        processIncomingSms(msg.originatingAddress, msg.messageBody)
                    }
                } else {
                    // Fallback to manual PDU parsing
                    val bundle = intent.extras
                    if (bundle != null) {
                        @Suppress("DEPRECATION")
                        val any = bundle.get("pdus")
                        if (any is Array<*>) {
                            val format = bundle.getString("format")
                            any.forEach { pdu ->
                                val msg = tryCreateFromPdu(pdu, format)
                                msg?.let { processIncomingSms(it.originatingAddress, it.messageBody) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun gzipDecompress(data: ByteArray): ByteArray {
        val bis = ByteArrayInputStream(data)
        GZIPInputStream(bis).use { gis ->
            val buffer = ByteArrayOutputStream()
            val tmp = ByteArray(8 * 1024)
            while (true) {
                val read = gis.read(tmp)
                if (read <= 0) break
                buffer.write(tmp, 0, read)
            }
            return buffer.toByteArray()
        }
    }

    private fun tryCreateFromPdu(pdu: Any?, format: String?): SmsMessage? {
        if (pdu !is ByteArray) return null
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !format.isNullOrBlank()) {
                SmsMessage.createFromPdu(pdu, format)
            } else {
                @Suppress("DEPRECATION")
                SmsMessage.createFromPdu(pdu)
            }
        } catch (_: Exception) { null }
    }

    private fun resolveSmsManager(): AndroidSmsManager {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val subId = SubscriptionManager.getDefaultSmsSubscriptionId()
                @Suppress("DEPRECATION")
                AndroidSmsManager.getSmsManagerForSubscriptionId(subId)
            } else {
                @Suppress("DEPRECATION")
                AndroidSmsManager.getDefault()
            }
        } catch (_: Exception) {
            @Suppress("DEPRECATION")
            AndroidSmsManager.getDefault()
        }
    }

    /**
     * Register SMS receiver
     */
    fun registerSmsReceiver() {
        val filter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        context.registerReceiver(smsReceiver, filter)
    }

    /**
     * Unregister SMS receiver
     */
    fun unregisterSmsReceiver() {
        context.unregisterReceiver(smsReceiver)
    }

    /**
     * Send an Mari protocol message via SMS
     */
    fun sendMariSms(phoneNumber: String, MariString: String): Boolean {
        return try {
            _smsState.value = SmsState.SENDING

            // Encrypt and compress the data
            val encryptedData = cryptoManager.encryptWithAuth(MariString.toByteArray())
            val combinedData = encryptedData.cipherText + encryptedData.iv + encryptedData.nonce + encryptedData.hmac
            val compressedData = gzipCompress(combinedData)

            // Encode as base64 for SMS compatibility
            val base64Data = Base64.getEncoder().encodeToString(compressedData)

            // Check if message fits in single SMS
            if (base64Data.length <= Mari_SMS_MAX_LENGTH - Mari_SMS_PREFIX.length) {
                // Send as single SMS
                val smsBody = Mari_SMS_PREFIX + base64Data
                sendSms(phoneNumber, smsBody)
            } else {
                // Send as multipart SMS
                sendMultipartSms(phoneNumber, base64Data)
            }

            _smsState.value = SmsState.SENT
            true
        } catch (e: Exception) {
            _smsState.value = SmsState.ERROR(e.message ?: "Unknown error")
            false
        }
    }

    /**
     * Public helper to parse an incoming SMS body that contains an Mari message.
     * Returns the decrypted Mari string if parsing succeeds; otherwise null.
     */
    fun parseIncomingSms(messageBody: String): String? {
        return when {
            messageBody.startsWith(Mari_SMS_PREFIX) -> {
                val data = messageBody.removePrefix(Mari_SMS_PREFIX)
                processSinglePartPayload(data)
            }
            messageBody.startsWith(Mari_RCPT_PREFIX) -> {
                // Receipt is plaintext: Mari_RCPT:amt=...;hash=...;role=...
                // We still surface it to listeners via receivedMessages; return null to avoid treating as coupon
                val receipt = parseReceipt(messageBody)
                receipt?.let {
                    val messageData = SmsMessageData(
                        sender = null,
                        message = messageBody,
                        timestamp = System.currentTimeMillis(),
                        isMariMessage = true
                    )
                    addReceivedMessage(messageData)
                }
                null
            }
            // Multipart requires accumulation across messages; return null here.
            messageBody.startsWith(Mari_SMS_MULTI_PART_PREFIX) -> null
            messageBody.startsWith(Mari_RCPT_PREFIX) -> null
            messageBody.startsWith("Mari_RCPTP_PART:") -> null
            else -> null
        }
    }

    /**
     * Send single SMS
     */
    private fun sendSms(phoneNumber: String, message: String) {
        // Note: PendingIntents omitted for brevity; can be added to track delivery.
        smsManager.sendTextMessage(phoneNumber, null, message, null, null)
    }

    /**
     * Send multipart SMS
     */
    private fun sendMultipartSms(phoneNumber: String, base64Data: String) {
        val partSize = Mari_SMS_MAX_LENGTH - Mari_SMS_MULTI_PART_PREFIX.length - 4 // Leave room for part info
        val parts = base64Data.chunked(partSize)
        val totalParts = parts.size

        parts.forEachIndexed { index, part ->
            val partMessage = "$Mari_SMS_MULTI_PART_PREFIX${index + 1}/$totalParts:$part"
            sendSms(phoneNumber, partMessage)
        }
    }

    /**
     * Process incoming SMS
     */
    private fun processIncomingSms(sender: String?, messageBody: String) {
        when {
            messageBody.startsWith(Mari_SMS_PREFIX) -> {
                val Mari = processSinglePartPayload(messageBody.removePrefix(Mari_SMS_PREFIX))
                if (Mari != null) {
                    val messageData = SmsMessageData(
                        sender = sender,
                        message = Mari,
                        timestamp = System.currentTimeMillis(),
                        isMariMessage = true
                    )
                    addReceivedMessage(messageData)
                }
            }
            messageBody.startsWith(Mari_RCPT_PREFIX) -> {
                if (!isSenderAllowed(sender)) return
                // Parse receipt and emit as a received message for observers to react (e.g., mark transaction as settled)
                val receipt = parseReceipt(messageBody)
                if (receipt != null) {
                    val messageData = SmsMessageData(
                        sender = sender,
                        message = messageBody,
                        timestamp = System.currentTimeMillis(),
                        isMariMessage = true
                    )
                    addReceivedMessage(messageData)
                }
            }
            messageBody.startsWith("Mari_RCPTP_PART:") -> {
                if (!isSenderAllowed(sender)) return
                // Format: Mari_RCPTP_PART:i/N:<base64>
                val regex = Regex("Mari_RCPTP_PART:(\\d+)/(\\d+):(.*)")
                val m = regex.find(messageBody)
                if (m != null) {
                    val (iStr, nStr, chunk) = m.destructured
                    val i = iStr.toInt(); val n = nStr.toInt()
                    val key = sender ?: "global"
                    val map = pendingProofParts.getOrPut(key) { mutableMapOf() }
                    map[i] = chunk
                    if (map.size == n) {
                        val b64 = buildString {
                            for (k in 1..n) append(map[k] ?: "")
                        }
                        pendingProofParts.remove(key)
                        // Verify proof offline in background
                        try {
                            val json = String(java.util.Base64.getDecoder().decode(b64))
                            val verifier = com.Mari.mobileapp.core.crypto.BankVerifier(context)
                            scope.launch {
                                val verified = verifier.verifyProofFromJson(json)
                                if (verified != null) {
                                    val messageData = SmsMessageData(
                                        sender = sender,
                                        message = "Mari_RCPTP_VERIFIED",
                                        timestamp = System.currentTimeMillis(),
                                        isMariMessage = true
                                    )
                                    addReceivedMessage(messageData)
                                }
                            }
                        } catch (_: Exception) { /* ignore */ }
                    }
                }
            }
            messageBody.startsWith(Mari_SMS_MULTI_PART_PREFIX) -> {
                processMultipartSms(sender, messageBody)
            }
        }
    }

    /**
     * Decode/decrypt a single-part payload
     */
    private fun processSinglePartPayload(base64Data: String): String? {
        return try {
            val compressedData = Base64.getDecoder().decode(base64Data)
            val combinedData = gzipDecompress(compressedData)

            // Extract components
            val cipherTextLength = combinedData.size - 32 - 12 - 16 // IV(12) + Nonce(16) + HMAC(32)
            if (cipherTextLength <= 0) {
                throw IllegalArgumentException("Invalid data length")
            }

            val cipherText = combinedData.copyOfRange(0, cipherTextLength)
            val iv = combinedData.copyOfRange(cipherTextLength, cipherTextLength + 12)
            val nonce = combinedData.copyOfRange(cipherTextLength + 12, cipherTextLength + 28)
            val hmac = combinedData.copyOfRange(cipherTextLength + 28, combinedData.size)

            val authenticatedData = MariCryptoManager.AuthenticatedEncryptedData(
                cipherText = cipherText,
                iv = iv,
                nonce = nonce,
                hmac = hmac
            )

            val decryptedData = cryptoManager.decryptWithAuth(authenticatedData)
            String(decryptedData, Charsets.UTF_8)
        } catch (e: Exception) {
            _smsState.value = SmsState.ERROR("Failed to process SMS: ${e.message}")
            null
        }
    }

    /**
     * Process multipart SMS
     */
    private fun processMultipartSms(sender: String?, messageBody: String) {
        val regex = Regex("${Mari_SMS_MULTI_PART_PREFIX}(\\d+)/(\\d+):(.*)")
        val matchResult = regex.find(messageBody)

        if (matchResult != null) {
            val (partNumber, totalParts, data) = matchResult.destructured
            val partNum = partNumber.toInt()
            val total = totalParts.toInt()

            // Create or update pending message
            val pendingMessage = pendingMessages.find { it.sender == sender && it.isMultipart }
                ?: SmsMessageData(
                    sender = sender,
                    message = "",
                    timestamp = System.currentTimeMillis(),
                    isMariMessage = true,
                    isMultipart = true,
                    totalParts = total
                ).also { pendingMessages.add(it) }

            // Update message parts
            pendingMessage.receivedParts[partNum] = data

            // Check if all parts received
            if (pendingMessage.receivedParts.size == total) {
                // Reassemble message
                val completeData = StringBuilder()
                for (i in 1..total) {
                    completeData.append(pendingMessage.receivedParts[i] ?: "")
                }

                // Process complete message
                val Mari = processSinglePartPayload(completeData.toString())
                if (Mari != null) {
                    val messageData = pendingMessage.copy(message = Mari)
                    addReceivedMessage(messageData)
                }
                pendingMessages.remove(pendingMessage)
            }
        }
    }

    /**
     * Add received message to list
     */
    private fun addReceivedMessage(messageData: SmsMessageData) {
        val currentMessages = _receivedMessages.value.toMutableList()
        currentMessages.add(messageData)
        _receivedMessages.value = currentMessages
    }

    /**
     * Check if SMS contains Mari protocol data
     */
    fun isMariSms(smsBody: String): Boolean {
        return smsBody.startsWith(Mari_SMS_PREFIX) ||
               smsBody.startsWith(Mari_SMS_MULTI_PART_PREFIX) ||
               smsBody.startsWith(Mari_RCPT_PREFIX)
    }

    private fun isSenderAllowed(sender: String?): Boolean {
        val cfg = BuildConfig.ALLOWED_SMS_SENDERS
        if (cfg.isNullOrBlank()) return true // no restriction configured
        val allowed = cfg.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (allowed.isEmpty()) return true
        return sender != null && allowed.any { it.equals(sender, ignoreCase = false) }
    }

    /**
     * Parse receipt message of the form: Mari_RCPT:amt=...;hash=...;role=...
     */
    fun parseReceipt(messageBody: String): Receipt? {
        return try {
            val raw = messageBody.removePrefix(Mari_RCPT_PREFIX)
            val parts = raw.split(';')
            val map = parts.mapNotNull {
                val kv = it.split('=')
                if (kv.size == 2) kv[0].trim() to kv[1].trim() else null
            }.toMap()
            val amt = map["amt"]?.toDoubleOrNull() ?: return null
            val hash = map["hash"] ?: return null
            val role = map["role"] ?: ""
            Receipt(amount = amt, shortHash = hash, role = role)
        } catch (_: Exception) {
            null
        }
    }

    data class Receipt(
        val amount: Double,
        val shortHash: String,
        val role: String
    )

    /**
     * Get received Mari messages
     */
    fun getReceivedMariMessages(): List<SmsMessageData> {
        return _receivedMessages.value.filter { it.isMariMessage }
    }

    /**
     * Clear received messages
     */
    fun clearReceivedMessages() {
        _receivedMessages.value = emptyList()
    }

    /**
     * Get SMS delivery status (stubbed as delivered)
     */
    @Suppress("UNUSED_PARAMETER")
    fun getDeliveryStatus(messageId: String): DeliveryStatus {
        return DeliveryStatus.DELIVERED
    }

    /**
     * SMS state types
     */
    sealed class SmsState {
        object IDLE : SmsState()
        object SENDING : SmsState()
        object SENT : SmsState()
        data class ERROR(val message: String) : SmsState()
    }

    /**
     * SMS message data
     */
    data class SmsMessageData(
        val sender: String?,
        val message: String,
        val timestamp: Long,
        val isMariMessage: Boolean = false,
        val isMultipart: Boolean = false,
        val totalParts: Int = 1,
        val receivedParts: MutableMap<Int, String> = mutableMapOf()
    )

    /**
     * Delivery status
     */
    enum class DeliveryStatus {
        PENDING,
        SENT,
        DELIVERED,
        FAILED
    }
}
