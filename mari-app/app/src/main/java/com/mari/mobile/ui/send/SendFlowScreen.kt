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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.Mari.mobile.ui.models.MotionData
import androidx.compose.runtime.collectAsState
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SendFlowScreen(
    onBack: () -> Unit,
    onComplete: () -> Unit,
    viewModel: com.Mari.mobile.ui.viewmodel.SendViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    var currentStep by remember { mutableStateOf(1) }
    var recipient by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var sendMode by remember { mutableStateOf("offline") }
    
    // Get location
    var location by remember { mutableStateOf<com.Mari.mobile.ui.viewmodel.LocationInfo?>(null) }
    LaunchedEffect(Unit) {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    location = com.Mari.mobile.ui.viewmodel.LocationInfo(it.latitude, it.longitude)
                }
            }
        } catch (e: Exception) {
            // Handle permission denial
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
                    onNext = { currentStep = 2 }
                )
                2 -> MotionSealScreen(
                    motionData = uiState.motionData,
                    onStartTracking = { viewModel.startMotionTracking() },
                    onStopTracking = { viewModel.stopMotionTracking() },
                    onNext = { 
                        viewModel.lookupRecipient(recipient)
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
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Transport Mode Selection
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline",
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text("Offline (SMS)", style = MaterialTheme.typography.bodySmall)
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = "Online",
                        modifier = Modifier.size(32.dp)
                    )
                    Text("Online (HTTP)", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        
        Text(
            text = "Recipient ID",
            style = MaterialTheme.typography.labelLarge
        )
        
        OutlinedTextField(
            value = recipient,
            onValueChange = onRecipientChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("0000001002") },
            trailingIcon = {
                IconButton(onClick = { /* QR Scanner */ }) {
                    Icon(Icons.Default.QrCode, contentDescription = "Scan QR")
                }
            }
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
            leadingIcon = { Text("¢", style = MaterialTheme.typography.bodyLarge) },
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
    onStartTracking: () -> Unit,
    onStopTracking: () -> Unit,
    onNext: () -> Unit
) {
    var isTracking by remember { mutableStateOf(false) }
    
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
                Text(
                    text = if (isTracking) "Shake your phone now!" else "Tap button below to start",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isTracking) MaterialTheme.colorScheme.primary 
                           else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isTracking) {
                    Spacer(modifier = Modifier.height(8.dp))
                    CircularProgressIndicator(
                        progress = ((motionData.x + motionData.y) / 200f).coerceIn(0f, 1f),
                        modifier = Modifier.size(48.dp)
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
            colors = if (isTracking) ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ) else ButtonDefaults.buttonColors()
        ) {
            Icon(
                imageVector = if (isTracking) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isTracking) "Stop Tracking" else "Start Tracking")
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
        
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = motionData.x > 50 && motionData.y > 50
        ) {
            Text("Verify & Generate Seal")
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
                DetailRow(label = "Amount", value = "¢${amount}")
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
                    text = "¢${String.format("%.2f", result.amount)} sent to ${result.recipient}",
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

