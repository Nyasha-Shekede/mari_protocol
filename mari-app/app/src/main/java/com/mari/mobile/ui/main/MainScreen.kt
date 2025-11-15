package com.Mari.mobile.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.Mari.mobile.ui.models.Transaction
import androidx.compose.runtime.collectAsState
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import android.Manifest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    onSendClick: () -> Unit,
    onLogout: () -> Unit,
    viewModel: com.Mari.mobile.ui.viewmodel.MainViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Location permission handling
    val locationPermissions = rememberMultiplePermissionsState(
        listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    // CONTINUOUS location updates while app is active
    val context = LocalContext.current
    LaunchedEffect(locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            
            // Create location request for continuous updates
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                10000L // Update every 10 seconds
            ).apply {
                setMinUpdateIntervalMillis(5000L) // But not faster than 5 seconds
                setMaxUpdateDelayMillis(15000L) // Max 15 seconds between updates
            }.build()
            
            // Location callback for continuous updates
            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                    result.lastLocation?.let { location ->
                        viewModel.updateLocation(location.latitude, location.longitude)
                    }
                }
            }
            
            try {
                // Start continuous location updates
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    android.os.Looper.getMainLooper()
                )
                
                // Also get immediate location
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        location?.let {
                            viewModel.updateLocation(it.latitude, it.longitude)
                        }
                    }
                
                // Cleanup when composable leaves composition
                kotlinx.coroutines.awaitCancellation()
            } catch (e: SecurityException) {
                // Permission denied
                locationPermissions.launchMultiplePermissionRequest()
            } catch (e: Exception) {
                // Other errors - try to get last known location
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        location?.let {
                            viewModel.updateLocation(it.latitude, it.longitude)
                        }
                    }
                } catch (_: Exception) {}
            } finally {
                // Stop location updates when leaving
                try {
                    fusedLocationClient.removeLocationUpdates(locationCallback)
                } catch (_: Exception) {}
            }
        } else {
            // Request permissions if not granted
            locationPermissions.launchMultiplePermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = uiState.userName.ifEmpty { "User" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Phone: ${uiState.userPhone.ifEmpty { "Loading..." }}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            // Balance Card
            item {
                BalanceCard(
                    balance = uiState.balance,
                    location = uiState.location,
                    onSendClick = onSendClick
                )
            }
            
            // QR Code Card
            item {
                QRCodeCard(
                    userPhone = uiState.userPhone,
                    bloodHash = "demo_user_${uiState.userPhone.take(10)}" // In production, use real bloodHash
                )
            }
            
            // Gamification Panel
            item {
                GamificationPanel(state = uiState.gamificationState)
            }
            
            // Recent Transactions Header
            item {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                )
            }
            
            // Transaction List
            items(uiState.transactions) { transaction ->
                TransactionItem(transaction = transaction)
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun BalanceCard(
    balance: Double,
    location: com.Mari.mobile.ui.viewmodel.LocationInfo?,
    onSendClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF6B337)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Available Balance",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Text(
                text = "R${String.format("%.2f", balance)}",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            
            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = location?.let { "Location: ${String.format("%.4f", it.lat)}, ${String.format("%.4f", it.lng)}" } ?: "Getting location...",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
            
            Button(
                onClick = onSendClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = Color(0xFFF6B337)
                )
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Send Money", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
fun GamificationPanel(state: com.Mari.mobile.ui.viewmodel.GamificationState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Weekly Challenge",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${state.txCountWeek}/50",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LinearProgressIndicator(
                progress = (state.txCountWeek / 50f).coerceIn(0f, 1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Points: ${state.pointsWeek}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (state.rewards.isNotEmpty()) {
                    Text(
                        text = "Reward unlocked ✓",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF4CAF50)
                    )
                } else {
                    Text(
                        text = "Unlock 100MB data at 50 tx",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (transaction.type == "received") 
                        Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (transaction.type == "received") 
                                Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (transaction.type == "received") 
                                Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Column {
                    Text(
                        text = "${if (transaction.type == "received") "Received from" else "Sent to"} ${transaction.peerName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "ID: ${transaction.peerId} • HSM #${transaction.hsmId}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${if (transaction.type == "received") "+" else "-"}R${String.format("%.2f", transaction.amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (transaction.type == "received") 
                        Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = transaction.timestamp,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun getDemoTransactions() = listOf(
    Transaction(
        id = "TXN7384",
        type = "received",
        amount = 120.0,
        peerId = "0000001002",
        peerName = "Jane Smith",
        timestamp = "2 minutes ago",
        hsmId = "AUR-4567"
    ),
    Transaction(
        id = "TXN1934",
        type = "sent",
        amount = 20.0,
        peerId = "0000001003",
        peerName = "Robert Johnson",
        timestamp = "1 day ago",
        hsmId = "AUR-1234"
    )
)


@Composable
fun QRCodeCard(
    userPhone: String,
    bloodHash: String
) {
    var showQR by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Your Payment QR Code",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Others can scan to pay you",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { showQR = !showQR }) {
                    Icon(
                        imageVector = if (showQR) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (showQR) "Hide QR" else "Show QR"
                    )
                }
            }
            
            if (showQR) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Generate QR code
                val qrBitmap = remember(bloodHash) {
                    com.Mari.mobile.ui.qr.QRCodeGenerator.generatePaymentQRCode(bloodHash, size = 400)
                }
                
                // Display QR code
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "Payment QR Code",
                    modifier = Modifier
                        .size(200.dp)
                        .background(Color.White, RoundedCornerShape(8.dp))
                        .padding(8.dp)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = userPhone,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "ID: ${bloodHash.take(16)}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
