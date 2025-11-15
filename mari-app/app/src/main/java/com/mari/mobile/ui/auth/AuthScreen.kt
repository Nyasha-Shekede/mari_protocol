package com.Mari.mobile.ui.auth

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.clickable
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
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isCameraOn by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var showDevMode by remember { mutableStateOf(false) }
    var devClickCount by remember { mutableStateOf(0) }
    
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
            // App Logo (tap 5 times for dev mode)
            Surface(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable {
                        devClickCount++
                        if (devClickCount >= 5) {
                            showDevMode = true
                        }
                    },
                color = if (showDevMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = "MariPay Logo",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "MariPay",
                style = MaterialTheme.typography.headlineLarge
            )
            
            Text(
                text = if (showDevMode) "🔧 DEV MODE ENABLED" else "Fast, secure payments with physics-based authentication",
                style = MaterialTheme.typography.bodyMedium,
                color = if (showDevMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
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
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isLoading = isLoading,
                            showDevMode = showDevMode,
                            onDevLogin = {
                                // Dev mode: instant login
                                onAuthSuccess()
                            },
                            onFaceIdClick = {
                                cameraLauncher.launch(Manifest.permission.CAMERA)
                                // TODO: Implement real face authentication
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
                                // TODO: Implement real registration with backend
                                // For now, just authenticate
                                onAuthSuccess()
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
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLoading: Boolean,
    showDevMode: Boolean,
    onDevLogin: () -> Unit,
    onFaceIdClick: () -> Unit,
    onBiometricClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (showDevMode) {
            // Dev Mode: Simple email/password login
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "🔧 DEV MODE",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = "Demo: demo@mari.com / demo123",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("demo@mari.com") }
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = password,
                onValueChange = onPasswordChange,
                label = { Text("Password") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("demo123") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onDevLogin,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Dev Login (Skip Auth)")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Or use biometric authentication below",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
        } else {
            Text(
                text = "Use your biometric authentication to unlock",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
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
        .setSubtitle("Confirm your identity to access MariPay")
        .setNegativeButtonText("Cancel")
        .build()
    
    biometricPrompt.authenticate(promptInfo)
}
