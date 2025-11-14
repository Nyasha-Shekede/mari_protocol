package com.Mari.mobile.ui.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var isCameraOn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    
    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCameraOn = true
        }
    }
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Logo
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                color = MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Mari Logo",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Mari Protocol",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Text(
                text = "Secure mobile banking with biometric authentication",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Login") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Register") }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    when (selectedTab) {
                        0 -> LoginTab(
                            isLoading = isLoading,
                            onFaceIdClick = {
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                                isLoading = true
                                // Simulate face auth
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(2500)
                                    isLoading = false
                                    onAuthSuccess()
                                }
                            },
                            onBiometricClick = {
                                authenticateWithBiometrics(context as FragmentActivity) { success ->
                                    if (success) onAuthSuccess()
                                }
                            }
                        )
                        1 -> RegisterTab(
                            name = name,
                            onNameChange = { name = it },
                            phone = phone,
                            onPhoneChange = { phone = it },
                            isCameraOn = isCameraOn,
                            onCameraToggle = {
                                if (!isCameraOn) {
                                    cameraLauncher.launch(Manifest.permission.CAMERA)
                                } else {
                                    isCameraOn = false
                                }
                            },
                            onRegister = {
                                isLoading = true
                                // Simulate registration
                                kotlinx.coroutines.GlobalScope.launch {
                                    kotlinx.coroutines.delay(1500)
                                    isLoading = false
                                    onAuthSuccess()
                                }
                            },
                            isLoading = isLoading
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoginTab(
    isLoading: Boolean,
    onFaceIdClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Use your biometric authentication to unlock",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onFaceIdClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Face, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLoading) "Authenticating..." else "Use Face ID")
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        OutlinedButton(
            onClick = onBiometricClick,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            Icon(Icons.Default.Fingerprint, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Use Biometrics")
        }
    }
}

@Composable
fun RegisterTab(
    name: String,
    onNameChange: (String) -> Unit,
    phone: String,
    onPhoneChange: (String) -> Unit,
    isCameraOn: Boolean,
    onCameraToggle: () -> Unit,
    onRegister: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = phone,
            onValueChange = onPhoneChange,
            label = { Text("Phone Number") },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onCameraToggle,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = if (isCameraOn) Icons.Default.VideocamOff else Icons.Default.Videocam,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isCameraOn) "Turn Off Camera" else "Capture Face")
        }
        
        if (isCameraOn) {
            Spacer(modifier = Modifier.height(16.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Camera Preview")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onRegister,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading && name.isNotBlank() && phone.isNotBlank() && isCameraOn
        ) {
            Icon(Icons.Default.PersonAdd, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (isLoading) "Registering..." else "Complete Registration")
        }
    }
}

fun authenticateWithBiometrics(
    activity: FragmentActivity,
    onResult: (Boolean) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val biometricPrompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(true)
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(false)
            }
            
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(false)
            }
        }
    )
    
    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Authenticate with Biometrics")
        .setSubtitle("Confirm your identity to access Mari")
        .setNegativeButtonText("Cancel")
        .build()
    
    biometricPrompt.authenticate(promptInfo)
}
