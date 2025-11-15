package com.Mari.mobile.ui.send

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.content.Context
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.hilt.navigation.compose.hiltViewModel
import com.Mari.mobile.ui.models.MotionData
import androidx.compose.runtime.collectAsState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendFlowScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: com.Mari.mobile.ui.viewmodel.SendViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var currentStep by remember { mutableStateOf(1) }
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    
    // Auto-detect network connectivity
    var sendMode by remember { mutableStateOf("offline") }
    var isOnline by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        isOnline = capabilities?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true &&
                   capabilities.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true
        sendMode = if (isOnline) "online" else "offline"
    }
    
    // CONTINUOUS location updates during send flow
    var location by remember { mutableStateOf<com.Mari.mobile.ui.viewmodel.LocationInfo?>(null) }
    LaunchedEffect(Unit) {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        
        // Create location request for continuous updates
        val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            5000L // Update every 5 seconds during send flow
        ).apply {
            setMinUpdateIntervalMillis(3000L) // Faster updates during transaction
            setMaxUpdateDelayMillis(10000L)
        }.build()
        
        // Location callback
        val locationCallback = object : com.google.android.gms.location.LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { loc ->
                    location = com.Mari.mobile.ui.viewmodel.LocationInfo(loc.latitude, loc.longitude)
                }
            }
        }
        
        try {
            // Start continuous updates
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
            
            // Get immediate location
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    loc?.let {
                        location = com.Mari.mobile.ui.viewmodel.LocationInfo(it.latitude, it.longitude)
                    }
                }
            
            kotlinx.coroutines.awaitCancellation()
        } catch (e: Exception) {
            // Fallback to last known
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                    loc?.let {
                        location = com.Mari.mobile.ui.viewmodel.LocationInfo(it.latitude, it.longitude)
                    }
                }
            } catch (_: Exception) {}
        } finally {
            // Cleanup
            try {
                fusedLocationClient.removeLocationUpdates(locationCallback)
            } catch (_: Exception) {}
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        when (currentStep) {
                            1 -> "Send Money"
                            2 -> "Create Security Seal"
                            3 -> "Confirm Payment"
                            4 -> "Transaction Status"
                            else -> "Send"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentStep) {
                1 -> RecipientAndAmountScreen(
                    recipient = recipient,
                    onRecipientChange = { recipient = it },
                    amount = amount,
                    onAmountChange = { amount = it },
                    sendMode = sendMode,
                    isOnline = isOnline,
                    onNext = { currentStep = 2 }
                )
                2 -> MotionSealScreen(
                    motionData = uiState.motionData,
                    hasLocation = location != null,
                    onStartTracking = { viewModel.startMotionTracking() },
                    onStopTracking = { viewModel.stopMotionTracking() },
                    onNext = { 
                        // Lookup recipient (online mode only)
                        viewModel.lookupRecipient(recipient, isOnline)
                        currentStep = 3 
                    }
                )
                3 -> ConfirmationScreen(
                    recipient = recipient,
                    recipientName = uiState.recipientName,
                    amount = amount,
                    location = location,
                    sendMode = sendMode,
                    isLoading = uiState.isLoading,
                    onConfirm = {
                        viewModel.sendTransaction(
                            recipientPhone = recipient,
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            motionData = uiState.motionData,
                            location = location,
                            mode = sendMode
                        )
                        currentStep = 4
                    }
                )
                4 -> ResultScreen(
                    result = uiState.transactionResult,
                    onDone = onComplete
                )
            }
        }
    }
}

@Composable
fun RecipientAndAmountScreen(
    recipient: String,
    onRecipientChange: (String) -> Unit,
    amount: String,
    onAmountChange: (String) -> Unit,
    sendMode: String,
    isOnline: Boolean,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Network status indicator
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isOnline) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                    contentDescription = null,
                    tint = if (isOnline) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (isOnline) "Online - Using HTTP" else "Offline - Using SMS",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isOnline) 
                        MaterialTheme.colorScheme.onPrimaryContainer 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Text(
            text = "Recipient Phone Number",
            style = MaterialTheme.typography.labelLarge
        )
        
        // Validate recipient format
        val isValidRecipient = remember(recipient) {
            when {
                recipient.isEmpty() -> true // Allow empty for typing
                recipient.matches(Regex("^\\d{10,15}$")) -> true // Phone: 10-15 digits
                recipient.matches(Regex("^[0-9a-fA-F]{64}$")) -> true // bloodHash: 64 hex chars
                else -> false
            }
        }
        
        OutlinedTextField(
            value = recipient,
            onValueChange = onRecipientChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("+1234567890") },
            supportingText = {
                if (!isValidRecipient) {
                    Text(
                        text = "Enter 10-15 digit phone number",
                        color = MaterialTheme.colorScheme.error
                    )
                } else if (recipient.length >= 10) {
                    Text(
                        text = "✓ Valid recipient ID",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            isError = !isValidRecipient,
            trailingIcon = {
                com.Mari.mobile.ui.qr.QRScannerButton(
                    onQRScanned = { scannedId ->
                        onRecipientChange(scannedId)
                    }
                )
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        
        Text(
            text = "Amount",
            style = MaterialTheme.typography.labelLarge
        )
        
        OutlinedTextField(
            value = amount,
            onValueChange = onAmountChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0.00") },
            leadingIcon = { Text("R", style = MaterialTheme.typography.bodyLarge) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = recipient.isNotBlank() && amount.isNotBlank()
        ) {
            Text("Continue")
        }
    }
}

@Composable
fun MotionSealScreen(
    motionData: MotionData,
    hasLocation: Boolean,
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onNext: () -> Unit
) {
    var isTracking by remember { mutableStateOf(false) }
    var timeAboveThreshold by remember { mutableStateOf(0L) }
    var startTime by remember { mutableStateOf(0L) }
    
    // Auto-advance when enough motion detected for 2 seconds
    LaunchedEffect(isTracking, motionData) {
        if (isTracking) {
            val totalMotion = motionData.x + motionData.y + motionData.z
            
            if (totalMotion >= 150f) {
                if (startTime == 0L) {
                    startTime = System.currentTimeMillis()
                } else {
                    val elapsed = System.currentTimeMillis() - startTime
                    timeAboveThreshold = elapsed
                    
                    // Auto-advance after 2 seconds of good shaking
                    if (elapsed >= 2000L) {
                        onStopTracking()
                        kotlinx.coroutines.delay(300) // Brief pause for UX
                        onNext()
                        isTracking = false
                        startTime = 0L
                        timeAboveThreshold = 0L
                    }
                }
            } else {
                // Reset timer if motion drops below threshold
                startTime = 0L
                timeAboveThreshold = 0L
            }
        } else {
            // Reset when not tracking
            startTime = 0L
            timeAboveThreshold = 0L
        }
    }
    
    DisposableEffect(Unit) {
        onDispose {
            if (isTracking) {
                onStopTracking()
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Create Security Seal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        // GPS REQUIRED WARNING
        if (!hasLocation) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(32.dp)
                    )
                    Column {
                        Text(
                            text = "GPS Required",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Text(
                            text = "Location is required for transaction security. Please enable GPS and wait for location lock.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        Text(
            text = "Shake your phone to create a unique motion seal for this transaction.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(
                    if (isTracking) MaterialTheme.colorScheme.primaryContainer 
                    else MaterialTheme.colorScheme.surfaceVariant,
                    RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = if (isTracking) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                val totalMotion = motionData.x + motionData.y + motionData.z
                val motionProgress = (totalMotion / 300f).coerceIn(0f, 1f)
                val timeProgress = (timeAboveThreshold / 2000f).coerceIn(0f, 1f)
                
                Text(
                    text = when {
                        !isTracking -> "Tap button below to start"
                        totalMotion < 50f -> "Shake harder! 💪"
                        totalMotion < 150f -> "Keep shaking... 📱"
                        timeAboveThreshold > 0L -> "Perfect! Hold it... ${2 - (timeAboveThreshold / 1000)}s"
                        else -> "Great! Keep going! 🎉"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isTracking) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isTracking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(contentAlignment = Alignment.Center) {
                        // Background progress (motion intensity)
                        CircularProgressIndicator(
                            progress = motionProgress,
                            modifier = Modifier.size(48.dp),
                            color = when {
                                totalMotion < 50f -> MaterialTheme.colorScheme.error
                                totalMotion < 150f -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            },
                            strokeWidth = 4.dp
                        )
                        // Foreground progress (time countdown)
                        if (timeAboveThreshold > 0L) {
                            CircularProgressIndicator(
                                progress = timeProgress,
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 6.dp
                            )
                        }
                    }
                    Text(
                        text = if (timeAboveThreshold > 0L) 
                            "${2 - (timeAboveThreshold / 1000)}s" 
                        else 
                            "${(motionProgress * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = {
                if (isTracking) {
                    onStopTracking()
                    isTracking = false
                } else {
                    onStartTracking()
                    isTracking = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = hasLocation, // REQUIRE GPS
            colors = if (isTracking) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ) else ButtonDefaults.buttonColors()
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else if (!hasLocation) Icons.Default.LocationOff else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                if (!hasLocation) "Waiting for GPS..."
                else if (isTracking) "Stop Tracking" 
                else "Start Tracking"
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            DataDisplay(label = "X", value = motionData.x.toInt().toString())
            DataDisplay(label = "Y", value = motionData.y.toInt().toString())
            DataDisplay(label = "Z", value = motionData.z.toInt().toString())
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "This motion signature is combined with your location and time to create a secure transaction seal.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(12.dp),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        val totalMotion = motionData.x + motionData.y + motionData.z
        val hasEnoughMotion = totalMotion >= 150f
        
        if (!hasEnoughMotion && totalMotion > 0) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Shake more vigorously to create a secure seal!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = hasEnoughMotion
        ) {
            Text(if (hasEnoughMotion) "Verify & Generate Seal" else "Shake to Continue (${(totalMotion / 150f * 100).toInt()}%)")
        }
    }
}

@Composable
fun DataDisplay(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun ConfirmationScreen(
    recipient: String,
    recipientName: String?,
    amount: String,
    location: com.Mari.mobile.ui.viewmodel.LocationInfo?,
    sendMode: String,
    isLoading: Boolean,
    onConfirm: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Confirm Payment",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold
        )
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DetailRow(label = "Recipient", value = recipientName ?: recipient)
                Divider()
                DetailRow(label = "Phone", value = recipient)
                Divider()
                DetailRow(label = "Amount", value = "R${amount}")
                Divider()
                DetailRow(
                    label = "Location", 
                    value = location?.let { "${String.format("%.4f", it.lat)}, ${String.format("%.4f", it.lng)}" } ?: "Unknown"
                )
                Divider()
                DetailRow(label = "Mode", value = if (sendMode == "offline") "SMS" else "HTTP")
            }
        }
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFFE8F5E9)
            )
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50)
                )
                Column {
                    Text(
                        text = "Security Seal Generated",
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Your unique transaction seal is ready.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isLoading) "Processing..." else "Confirm & Send Payment")
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ResultScreen(
    result: com.Mari.mobile.ui.viewmodel.TransactionResult?,
    onDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (result) {
            is com.Mari.mobile.ui.viewmodel.TransactionResult.Success -> {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color(0xFFE8F5E9)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Payment Submitted",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "R${String.format("%.2f", result.amount)} sent to ${result.recipient}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Transaction ID",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = result.id,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Security Seal Hash",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = result.hash,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            is com.Mari.mobile.ui.viewmodel.TransactionResult.Pending -> {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color(0xFFFFF3E0)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = Color(0xFFF6B337),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Payment Queued",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = result.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            is com.Mari.mobile.ui.viewmodel.TransactionResult.Failure -> {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = RoundedCornerShape(40.dp),
                    color = Color(0xFFFFEBEE)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Text(
                    text = "Payment Failed",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = result.error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            null -> {}
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Button(
            onClick = onDone,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Done")
        }
    }
}

