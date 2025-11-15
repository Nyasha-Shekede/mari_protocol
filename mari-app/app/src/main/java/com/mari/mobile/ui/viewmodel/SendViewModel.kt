package com.Mari.mobile.ui.viewmodel

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.Mari.mobile.ui.models.MotionData
import com.Mari.mobileapp.core.transaction.TransactionManager
import com.Mari.mobileapp.domain.model.Transaction
import com.Mari.mobileapp.domain.model.TransactionStatus
import com.Mari.mobileapp.domain.model.TransactionType
import com.Mari.mobileapp.domain.model.TransportMethod
import com.Mari.mobileapp.domain.repository.TransactionRepository
import com.Mari.mobileapp.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.sqrt

@HiltViewModel
class SendViewModel @Inject constructor(
    private val transactionManager: TransactionManager,
    private val transactionRepository: TransactionRepository,
    private val userRepository: UserRepository,
    @ApplicationContext private val context: Context
) : ViewModel(), SensorEventListener {
    
    private val _uiState = MutableStateFlow(SendUiState())
    val uiState: StateFlow<SendUiState> = _uiState.asStateFlow()
    
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    // Motion tracking state
    private var lastX = 0f
    private var lastY = 0f
    private var lastZ = 0f
    private var lastTimestamp = 0L
    private var accumulatedMotion = MotionData(0f, 0f, 0f)
    private var isTrackingMotion = false
    
    fun lookupRecipient(phone: String) {
        _uiState.update {
            it.copy(
                recipientName = "User $phone",
                recipientBioHash = phone,
                isLoading = false,
                error = null
            )
        }
    }
    
    fun startMotionTracking() {
        isTrackingMotion = true
        accumulatedMotion = MotionData(0f, 0f, 0f)
        lastTimestamp = 0L
        
        accelerometer?.let {
            sensorManager.registerListener(
                this,
                it,
                SensorManager.SENSOR_DELAY_GAME
            )
        }
    }
    
    fun stopMotionTracking() {
        isTrackingMotion = false
        sensorManager.unregisterListener(this)
    }
    
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTrackingMotion) return
        
        event?.let {
            val currentNanos = System.nanoTime()
            val currentMillis = System.currentTimeMillis()
            
            if (lastTimestamp != 0L) {
                val x = it.values[0]
                val y = it.values[1]
                val z = it.values[2]
                
                // Calculate delta from previous reading
                val deltaX = kotlin.math.abs(x - lastX)
                val deltaY = kotlin.math.abs(y - lastY)
                val deltaZ = kotlin.math.abs(z - lastZ)
                
                // Motion threshold to filter out STATIC sensor noise only (0.8 m/s²)
                val STATIC_NOISE_THRESHOLD = 0.8f
                val totalDelta = deltaX + deltaY + deltaZ
                
                // Only accumulate if motion exceeds static noise threshold
                if (totalDelta > STATIC_NOISE_THRESHOLD) {
                    // Calculate magnitude of acceleration
                    val magnitude = sqrt(x * x + y * y + z * z)
                    
                    // HIGH ENTROPY CAPTURE - Include timing chaos
                    val timingEntropy = (currentNanos % 1000).toFloat() / 1000f  // Nanosecond chaos
                    val millisEntropy = (currentMillis % 100).toFloat() / 100f   // Millisecond variation
                    
                    // Chaotic amplification - small differences become huge
                    // Each shake is unique due to:
                    // 1. Exact timing (nanosecond precision)
                    // 2. Acceleration patterns (impossible to reproduce exactly)
                    // 3. Magnitude variations (human hand tremor)
                    val chaoticX = deltaX * (10f + timingEntropy * 5f)  // 10-15x amplification
                    val chaoticY = deltaY * (10f + millisEntropy * 5f)  // 10-15x amplification
                    val chaoticZ = magnitude * (0.5f + timingEntropy * 0.3f)  // 0.5-0.8x
                    
                    // Accumulate with chaotic amplification
                    accumulatedMotion = accumulatedMotion.copy(
                        x = accumulatedMotion.x + chaoticX,
                        y = accumulatedMotion.y + chaoticY,
                        z = accumulatedMotion.z + chaoticZ
                    )
                    
                    _uiState.update { it.copy(motionData = accumulatedMotion) }
                }
            }
            
            lastX = it.values[0]
            lastY = it.values[1]
            lastZ = it.values[2]
            lastTimestamp = currentMillis
        }
    }
    
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // Not needed for this use case
    }
    
    fun sendTransaction(
        recipientPhone: String,
        amount: Double,
        motionData: MotionData,
        location: LocationInfo?,
        mode: String
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                val recipient = _uiState.value.recipientBioHash
                    ?: throw IllegalStateException("Recipient not found")
                
                // Get current user's bioHash from repository
                val currentUser = userRepository.getCurrentUser()
                    ?: throw IllegalStateException("User not authenticated")
                
                // Send payment using TransactionManager (immediate settlement, no delays)
                val result = transactionManager.sendPayment(recipient, amount)
                
                when (result) {
                    is TransactionManager.TransactionResult.Success -> {
                        // Save transaction to database
                        val transaction = Transaction(
                            id = java.util.UUID.randomUUID().toString(),
                            senderBioHash = currentUser.bloodHash,
                            receiverBioHash = recipient,
                            amount = amount,
                            locationGrid = location?.let { "${it.lat},${it.lng}" } ?: "unknown",
                            timestamp = System.currentTimeMillis(),
                            status = TransactionStatus.COMPLETED, // Immediate settlement
                            type = TransactionType.SEND,
                            coupon = result.coupon,
                            transportMethod = when (result.method) {
                                TransactionManager.TransportMethod.SMS -> TransportMethod.SMS
                                TransactionManager.TransportMethod.INTERNET -> TransportMethod.RECEIVED
                                TransactionManager.TransportMethod.RECEIVED -> TransportMethod.RECEIVED
                            }
                        )
                        
                        transactionRepository.createTransaction(transaction)
                        
                        _uiState.update {
                            it.copy(
                                transactionResult = TransactionResult.Success(
                                    id = transaction.id,
                                    amount = amount,
                                    recipient = recipientPhone,
                                    hash = result.coupon.take(32)
                                ),
                                isLoading = false
                            )
                        }
                    }
                    is TransactionManager.TransactionResult.Error -> {
                        _uiState.update {
                            it.copy(
                                error = result.message,
                                isLoading = false
                            )
                        }
                    }
                    is TransactionManager.TransactionResult.Queued -> {
                        // Even queued transactions are treated as pending, not settled
                        _uiState.update {
                            it.copy(
                                transactionResult = TransactionResult.Pending(
                                    message = "Transaction queued for processing"
                                ),
                                isLoading = false
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        error = e.message ?: "Transaction failed",
                        isLoading = false
                    )
                }
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        stopMotionTracking()
    }
}

data class SendUiState(
    val recipientName: String? = null,
    val recipientBioHash: String? = null,
    val motionData: MotionData = MotionData(),
    val transactionResult: TransactionResult? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

sealed class TransactionResult {
    data class Success(
        val id: String,
        val amount: Double,
        val recipient: String,
        val hash: String
    ) : TransactionResult()
    
    data class Pending(
        val message: String
    ) : TransactionResult()
    
    data class Failure(
        val error: String
    ) : TransactionResult()
}
