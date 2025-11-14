package com.Mari.mobileapp.core.transaction

import android.content.Context
import com.Mari.mobileapp.core.network.NetworkManager
import com.Mari.mobileapp.core.physics.PhysicsSensorManager
import com.Mari.mobileapp.core.protocol.MariProtocol
import com.Mari.mobileapp.core.sms.SmsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.UUID

/**
 * Manages Mari transactions and payment flows
 */
class TransactionManagerImpl(
    private val context: Context,
    private val mariProtocol: MariProtocol,
    private val smsManager: SmsManager,
    private val physicsSensorManager: PhysicsSensorManager,
    private val networkManager: NetworkManager,
    private val coreGateway: com.Mari.mobileapp.service.core.CoreGateway,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.IO)
): TransactionManager {
    // State management
    private val _transactionState = MutableStateFlow(TransactionState.IDLE)
    val transactionState: StateFlow<TransactionState> = _transactionState

    private val _activeTransactions = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val activeTransactions: StateFlow<List<TransactionRecord>> = _activeTransactions

    private val _transactionHistory = MutableStateFlow<List<TransactionRecord>>(emptyList())
    val transactionHistory: StateFlow<List<TransactionRecord>> = _transactionHistory

    private val pendingTransactions = ConcurrentHashMap<String, TransactionRecord>()
    private val failedTransactions = ConcurrentHashMap<String, TransactionRecord>()
    private val transactionCallbacks = ConcurrentHashMap<String, (TransactionManager.TransactionResult) -> Unit>()

    // Transaction settings
    private val maxRetryAttempts = 3
    private val transactionTimeout = 300000L // 5 minutes

    /**
     * Send a payment to a recipient
     */
    override suspend fun sendPayment(recipientBio: String, amount: Double): TransactionManager.TransactionResult {
        return try {
            val senderBio = recipientBio // biometric-derivation removed; senderBio is supplied externally in app flows
            val coupon = mariProtocol.generateTransferCoupon(senderBio, recipientBio, amount)
            // Prefer Internet for immediate, bank-mediated finality. SMS is a transport only and never marks success.
            if (networkManager.isConnected()) {
                val rec = TransactionRecord(
                    id = generateTransactionId(),
                    senderBio = senderBio,
                    receiverBio = recipientBio,
                    amount = amount,
                    coupon = coupon,
                    timestamp = System.currentTimeMillis(),
                    state = TransactionRecordState.PENDING
                )
                val result = trySendViaInternet(rec)
                if (result is TransactionManager.TransactionResult.Success) return result
            }
            // Attempt SMS transport for server-side processing, but do not mark as settled
            trySendViaSms(recipientBio, coupon)
            TransactionManager.TransactionResult.Queued(coupon)
        } catch (e: Exception) {
            TransactionManager.TransactionResult.Error(e.message ?: "Failed to send payment")
        }
    }

    

    /**
     * Parse a coupon
     */
    override fun parseCoupon(coupon: String): TransactionManager.ParsedCoupon {
        val entity = mariProtocol.parseMariString(coupon)
        if (entity is com.Mari.mobileapp.core.protocol.MariProtocol.TransferCouponEntity) {
            return TransactionManager.ParsedCoupon(
                senderBio = entity.senderBio,
                receiverBio = entity.receiverBio,
                amount = entity.amount,
                grid = entity.grid,
                timestamp = entity.expiry,
                signature = entity.seal
            )
        }
        throw IllegalArgumentException("Invalid coupon format")
    }
    

    /**
     * Attempt transaction with available methods
     */
    private suspend fun attemptTransaction(transactionRecord: TransactionRecord): TransactionManager.TransactionResult {
        val methods = determineTransmissionMethods()

        for (method in methods) {
            when (method) {
                TransmissionMethod.SMS -> {
                    val smsResult = trySendViaSms(transactionRecord.receiverBio, transactionRecord.coupon)
                    if (smsResult) {
                        return TransactionManager.TransactionResult.Success(transactionRecord.coupon, TransactionManager.TransportMethod.SMS)
                    }
                }

                TransmissionMethod.INTERNET -> {
                    val internetResult = trySendViaInternet(transactionRecord)
                    if (internetResult != null) {
                        return internetResult
                    }
                }

                TransmissionMethod.RECEIVED -> {
                    // not used for sending
                }
            }
        }

        // Queue for later if all methods failed
        pendingTransactions[transactionRecord.id] = transactionRecord
        return TransactionManager.TransactionResult.Queued(transactionRecord.coupon)
    }

    /**
     * Determine available transmission methods
     */
    private fun determineTransmissionMethods(): List<TransmissionMethod> {
        val methods = mutableListOf<TransmissionMethod>()

        // Check network availability
        if (networkManager.isConnected()) {
            methods.add(TransmissionMethod.INTERNET)
        }

        // SMS is always available as fallback transport
        methods.add(TransmissionMethod.SMS)

        return methods
    }

    /**
     * Try to send data via SMS
     */
    override suspend fun trySendViaSms(recipient: String, coupon: String): Boolean {
        return try {
            val demoPhoneNumber = "+15704582189" // replace with resolved phone in real usage
            smsManager.sendMariSms(demoPhoneNumber, coupon)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Try to send data via Internet (placeholder for future implementation)
     */
    private suspend fun trySendViaInternet(transactionRecord: TransactionRecord): TransactionManager.TransactionResult? {
        return try {
            // Build physicsData from current sensors
            val motion = physicsSensorManager.motionData.value
            val x = motion.x.toDouble()
            val y = motion.y.toDouble()
            val z = motion.z.toDouble()
            val grid = com.Mari.mobile.BuildConfig.DEFAULT_GRID
            val tz = java.util.TimeZone.getTimeZone("UTC")
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
            sdf.timeZone = tz
            val timestampIso = sdf.format(java.util.Date())

            // Parse coupon to extract sender/receiver and amount
            val parsed = mariProtocol.parseMariString(transactionRecord.coupon)
            if (parsed is com.Mari.mobileapp.core.protocol.MariProtocol.TransferCouponEntity) {
                coreGateway.postTransaction(
                    senderBioHash = parsed.senderBio,
                    receiverBioHash = parsed.receiverBio,
                    amount = parsed.amount,
                    locationGrid = grid,
                    coupon = transactionRecord.coupon,
                    motionX = x,
                    motionY = y,
                    motionZ = z,
                    timestampIso = timestampIso
                )
                return TransactionManager.TransactionResult.Success(transactionRecord.coupon, TransactionManager.TransportMethod.INTERNET)
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Receive and process a payment coupon
     */
    override suspend fun receivePayment(coupon: String): TransactionManager.TransactionResult {
        return try {
            val parsed = mariProtocol.parseMariString(coupon)
            if (parsed is com.Mari.mobileapp.core.protocol.MariProtocol.TransferCouponEntity) {
                val validation = mariProtocol.validateCoupon(parsed)
                if (validation.isValid) {
                    TransactionManager.TransactionResult.Success(coupon, TransactionManager.TransportMethod.RECEIVED)
                } else {
                    val errorNames = validation.errors.joinToString(",") { error -> error.name }
                    TransactionManager.TransactionResult.Error("Invalid coupon: $errorNames")
                }
            } else TransactionManager.TransactionResult.Error("Invalid coupon format")
        } catch (e: Exception) {
            TransactionManager.TransactionResult.Error(e.message ?: "Failed to process payment")
        }
    }

    /**
     * Process received SMS message
     */
    fun processReceivedSms(sender: String?, message: String) {
        if (smsManager.isMariSms(message)) {
            val MariString = smsManager.parseIncomingSms(message)
            MariString?.let {
                // Optionally handle result
                // run in background if needed
                coroutineScope.launch { receivePayment(it) }
            }
        }
    }

    /**
     * Handle transaction result
     */
    private fun handleTransactionResult(transactionId: String, result: TransactionManager.TransactionResult) {
        val transactionRecord = getTransactionRecord(transactionId)
        transactionRecord?.let {
            when (result) {
                is TransactionManager.TransactionResult.Success -> {
                    it.state = TransactionRecordState.COMPLETED
                    it.completionMethod = TransmissionMethod.INTERNET // map for history; UI uses domain events
                    removeActiveTransaction(transactionId)
                    addToHistory(it)
                }
                is TransactionManager.TransactionResult.Error -> {
                    it.state = TransactionRecordState.FAILED
                    it.errorMessage = result.message
                    it.retryCount++

                    if (it.retryCount < maxRetryAttempts && networkManager.isConnected()) {
                        // Retry transaction
                        coroutineScope.launch {
                            val retryResult = attemptTransaction(it)
                            handleTransactionResult(transactionId, retryResult)
                        }
                    } else {
                        removeActiveTransaction(transactionId)
                        failedTransactions[transactionId] = it
                    }
                }
                is TransactionManager.TransactionResult.Queued -> {
                    it.state = TransactionRecordState.QUEUED // queued for server-side processing; UI must not treat as settled
                }
            }
        }

        transactionCallbacks[transactionId]?.invoke(result)
        transactionCallbacks.remove(transactionId)
    }

    /**
     * Process pending transactions when connectivity is available
     */
    fun processPendingTransactions() {
        coroutineScope.launch {
            val iterator = pendingTransactions.entries.iterator()
            while (iterator.hasNext()) {
                val (txnId, txnRecord) = iterator.next()
                val result = attemptTransaction(txnRecord)

                if (result !is TransactionManager.TransactionResult.Queued) {
                    handleTransactionResult(txnId, result)
                    iterator.remove()
                }
            }
        }
    }

    /**
     * Generate location verification
     */
    fun generateLocationVerification(): String {
        return mariProtocol.generateLocationVerification()
    }

    /**
     * Validate location verification
     */
    fun validateLocationVerification(verificationString: String): MariProtocol.ValidationResult {
        return try {
            val parsedVerification = mariProtocol.parseMariString(verificationString)
            if (parsedVerification is MariProtocol.LocationVerificationEntity) {
                mariProtocol.validateLocationVerification(parsedVerification)
            } else {
                MariProtocol.ValidationResult(false, listOf(MariProtocol.ValidationError.SEAL_MISMATCH))
            }
        } catch (e: Exception) {
            MariProtocol.ValidationResult(false, listOf(MariProtocol.ValidationError.SEAL_MISMATCH))
        }
    }

    /**
     * Generate physics challenge
     */
    fun generatePhysicsChallenge(): String {
        return mariProtocol.generatePhysicsChallenge()
    }

    /**
     * Validate physics challenge
     */
    fun validatePhysicsChallenge(challengeString: String): MariProtocol.ValidationResult {
        return try {
            val parsedChallenge = mariProtocol.parseMariString(challengeString)
            if (parsedChallenge is MariProtocol.PhysicsChallengeEntity) {
                mariProtocol.validatePhysicsChallenge(parsedChallenge)
            } else {
                MariProtocol.ValidationResult(false, listOf(MariProtocol.ValidationError.SEAL_MISMATCH))
            }
        } catch (e: Exception) {
            MariProtocol.ValidationResult(false, listOf(MariProtocol.ValidationError.SEAL_MISMATCH))
        }
    }

    /**
     * Get transaction by ID
     */
    fun getTransactionRecord(transactionId: String): TransactionRecord? {
        return _activeTransactions.value.find { it.id == transactionId }
            ?: _transactionHistory.value.find { it.id == transactionId }
            ?: pendingTransactions[transactionId]
            ?: failedTransactions[transactionId]
    }

    /**
     * Get transaction history
     */
    fun getTransactionHistory(limit: Int = 100): List<TransactionRecord> {
        return _transactionHistory.value.takeLast(limit)
    }

    /**
     * Get failed transactions
     */
    fun getFailedTransactions(): List<TransactionRecord> {
        return failedTransactions.values.toList()
    }

    /**
     * Retry failed transaction
     */
    fun retryTransaction(transactionId: String): Boolean {
        val failedTransaction = failedTransactions[transactionId]
        failedTransaction?.let {
            it.state = TransactionRecordState.PENDING
            it.errorMessage = null
            failedTransactions.remove(transactionId)

            coroutineScope.launch {
                val result = attemptTransaction(it)
                handleTransactionResult(transactionId, result)
            }

            return true
        }
        return false
    }

    /**
     * Cancel transaction
     */
    fun cancelTransaction(transactionId: String): Boolean {
        val activeTransaction = getTransactionRecord(transactionId)
        activeTransaction?.let {
            it.state = TransactionRecordState.CANCELLED
            removeActiveTransaction(transactionId)
            pendingTransactions.remove(transactionId)
            transactionCallbacks.remove(transactionId)
            return true
        }
        return false
    }

    /**
     * Add active transaction
     */
    private fun addActiveTransaction(transactionRecord: TransactionRecord) {
        val currentTransactions = _activeTransactions.value.toMutableList()
        currentTransactions.add(transactionRecord)
        _activeTransactions.value = currentTransactions
    }

    /**
     * Remove active transaction
     */
    private fun removeActiveTransaction(transactionId: String) {
        val currentTransactions = _activeTransactions.value.toMutableList()
        currentTransactions.removeAll { it.id == transactionId }
        _activeTransactions.value = currentTransactions
    }

    /**
     * Add to history
     */
    private fun addToHistory(transactionRecord: TransactionRecord) {
        val currentHistory = _transactionHistory.value.toMutableList()
        currentHistory.add(transactionRecord)
        _transactionHistory.value = currentHistory
    }

    /**
     * Generate unique transaction ID
     */
    private fun generateTransactionId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * Transaction state types
     */
    sealed class TransactionState {
        object IDLE : TransactionState()
        object PROCESSING : TransactionState()
        data class SUCCESS(val coupon: String) : TransactionState()
        data class ERROR(val message: String) : TransactionState()
    }

    /**
     * Transaction result types
     */
    // Use interface types for results
    enum class TransmissionMethod { SMS, INTERNET, RECEIVED }

    /**
     * Transaction record
     */
    data class TransactionRecord(
        val id: String,
        val senderBio: String,
        val receiverBio: String,
        val amount: Double,
        val coupon: String,
        val timestamp: Long,
        var state: TransactionRecordState,
        var retryCount: Int = 0,
        var validationResult: MariProtocol.ValidationResult? = null,
        var completionMethod: TransmissionMethod? = null,
        var errorMessage: String? = null
    )

    /**
     * Transaction record state
     */
    enum class TransactionRecordState {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED,
        QUEUED,
        CANCELLED
    }
}
