package com.Mari.mobile.ui.qr

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

/**
 * QR Code scanner for Mari user IDs
 */
@Composable
fun QRScannerButton(
    onQRScanned: (String) -> Unit
) {
    val scanLauncher = rememberLauncherForActivityResult(
        contract = ScanContract()
    ) { result ->
        result.contents?.let { scannedData ->
            // Parse Mari QR format: Mari://pay?to=<bloodHash>&amount=<optional>
            val userId = when {
                scannedData.startsWith("Mari://pay?to=") -> {
                    val params = scannedData.substringAfter("?")
                    val toParam = params.split("&").find { it.startsWith("to=") }
                    toParam?.substringAfter("=") ?: scannedData
                }
                else -> scannedData // Assume raw bloodHash or phone number
            }
            onQRScanned(userId)
        }
    }
    
    val scanOptions = ScanOptions().apply {
        setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        setPrompt("Scan Mari QR Code")
        setBeepEnabled(true)
        setOrientationLocked(true)
        setCaptureActivity(CustomCaptureActivity::class.java)
    }
    
    IconButton(
        onClick = { scanLauncher.launch(scanOptions) }
    ) {
        Icon(Icons.Default.QrCode, contentDescription = "Scan QR Code")
    }
}
