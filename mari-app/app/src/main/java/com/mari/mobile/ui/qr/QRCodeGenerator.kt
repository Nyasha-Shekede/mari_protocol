package com.Mari.mobile.ui.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * QR Code generator for Mari user IDs
 */
object QRCodeGenerator {
    
    /**
     * Generate QR code bitmap from user ID (bloodHash)
     * 
     * @param userId The bloodHash or phone number to encode
     * @param size Size of the QR code in pixels
     * @return Bitmap of the QR code
     */
    fun generateQRCode(userId: String, size: Int = 512): Bitmap {
        val hints = hashMapOf<EncodeHintType, Any>().apply {
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H) // High error correction
            put(EncodeHintType.MARGIN, 1) // Minimal margin
        }
        
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(
            userId,
            BarcodeFormat.QR_CODE,
            size,
            size,
            hints
        )
        
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        
        return bitmap
    }
    
    /**
     * Generate QR code for Mari payment request
     * Format: Mari://pay?to=<bloodHash>&amount=<optional>
     */
    fun generatePaymentQRCode(bloodHash: String, amount: Double? = null, size: Int = 512): Bitmap {
        val qrData = if (amount != null) {
            "Mari://pay?to=$bloodHash&amount=$amount"
        } else {
            "Mari://pay?to=$bloodHash"
        }
        return generateQRCode(qrData, size)
    }
}
