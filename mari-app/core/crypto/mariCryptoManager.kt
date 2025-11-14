package com.Mari.mobileapp.core.crypto

import com.Mari.mobileapp.core.physics.PhysicsSensorManager
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Handles all cryptographic operations for the Mari protocol
 */
class MariCryptoManager(
    private val physicsSensorManager: PhysicsSensorManager
) {
    companion object {
        private const val AES_KEY_SIZE = 256
        private const val GCM_TAG_LENGTH = 128
        private const val GCM_IV_LENGTH = 12 // 96 bits

        // KDF constants
        private const val KDF_SALT = "Mari_KDF_V1"
        private const val KDF_ITERATIONS = 10000

        // Additional constants for enhanced security
        private const val HMAC_KEY_SIZE = 256
        private const val NONCE_SIZE = 16
    }

    private val secureRandom = SecureRandom()

    /**
     * Generate a physics-bound encryption key
     */
    fun generatePhysicsKey(): SecretKeySpec {
        val seed = physicsSensorManager.generatePhysicsSeed()
        val kdfInput = seed.toString() + KDF_SALT + System.currentTimeMillis()
        val hash = sha256(kdfInput.toByteArray())

        // Apply PBKDF2-like key stretching
        var stretchedKey = hash
        for (i in 0 until KDF_ITERATIONS) {
            stretchedKey = sha256(stretchedKey + i.toByte())
        }

        return SecretKeySpec(stretchedKey.copyOf(32), "AES")
    }

    /**
     * Generate IV from accelerometer data
     */
    private fun generateIvFromAccelerometer(): ByteArray {
        val motion = physicsSensorManager.motionData.value
        val gyro = physicsSensorManager.gyroscopeData.value
        val magnetic = physicsSensorManager.magneticData.value
        val time = System.nanoTime()

        val ivData = "${'$'}{motion.x}:${'$'}{motion.y}:${'$'}{motion.z}:${'$'}{gyro.x}:${'$'}{gyro.y}:${'$'}{gyro.z}:${'$'}{magnetic.x}:${'$'}{magnetic.y}:${'$'}{magnetic.z}:${'$'}time"
        return sha256(ivData.toByteArray()).copyOfRange(0, GCM_IV_LENGTH)
    }

    /**
     * Generate a random nonce
     */
    fun generateNonce(): ByteArray {
        val nonce = ByteArray(NONCE_SIZE)
        secureRandom.nextBytes(nonce)
        return nonce
    }

    /**
     * Encrypt data using physics-bound AES-GCM
     */
    fun encryptData(data: ByteArray): EncryptedData {
        val key = generatePhysicsKey()
        val iv = generateIvFromAccelerometer()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)

        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec)
        val cipherText = cipher.doFinal(data)

        return EncryptedData(cipherText, iv, generateNonce())
    }

    /**
     * Decrypt data using physics-bound AES-GCM
     */
    fun decryptData(encryptedData: EncryptedData): ByteArray {
        val key = generatePhysicsKey()

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val parameterSpec = GCMParameterSpec(GCM_TAG_LENGTH, encryptedData.iv)

        cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec)
        return cipher.doFinal(encryptedData.cipherText)
    }

    /**
     * Generate a seal for data integrity verification
     */
    fun generateSeal(): String {
        val motion = physicsSensorManager.motionData.value
        val light = physicsSensorManager.lightLevel.value
        val gyro = physicsSensorManager.gyroscopeData.value
        val magnetic = physicsSensorManager.magneticData.value
        val temp = physicsSensorManager.deviceTemperature.value
        val time = System.nanoTime()

        val sealData = "${'$'}{motion.x}:${'$'}{motion.y}:${'$'}{motion.z}:${'$'}light:${'$'}{gyro.x}:${'$'}{gyro.y}:${'$'}{gyro.z}:${'$'}{magnetic.x}:${'$'}{magnetic.y}:${'$'}{magnetic.z}:${'$'}temp:${'$'}time"
        val hash = sha256(sealData.toByteArray())

        // Use first 8 bytes of hash as seal
        return hash.copyOfRange(0, 8).joinToString("") { "%02x".format(it) }
    }

    /**
     * Generate HMAC for additional authentication
     */
    fun generateHmac(data: ByteArray): ByteArray {
        val key = generatePhysicsKey()
        val hmacData = key.encoded + data
        return sha256(hmacData)
    }

    /**
     * Encrypt with additional authentication
     */
    fun encryptWithAuth(data: ByteArray): AuthenticatedEncryptedData {
        val encryptedData = encryptData(data)
        val hmac = generateHmac(encryptedData.cipherText)

        return AuthenticatedEncryptedData(
            cipherText = encryptedData.cipherText,
            iv = encryptedData.iv,
            nonce = encryptedData.nonce,
            hmac = hmac
        )
    }

    /**
     * Verify and decrypt authenticated data
     */
    fun decryptWithAuth(authenticatedData: AuthenticatedEncryptedData): ByteArray {
        val expectedHmac = generateHmac(authenticatedData.cipherText)

        // Verify HMAC
        if (!expectedHmac.contentEquals(authenticatedData.hmac)) {
            throw SecurityException("HMAC verification failed")
        }

        val encryptedData = EncryptedData(
            cipherText = authenticatedData.cipherText,
            iv = authenticatedData.iv,
            nonce = authenticatedData.nonce
        )

        return decryptData(encryptedData)
    }

    /**
     * SHA-256 hash function
     */
    private fun sha256(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-256").digest(data)
    }

    /**
     * SHA-512 hash function for enhanced security
     */
    private fun sha512(data: ByteArray): ByteArray {
        return MessageDigest.getInstance("SHA-512").digest(data)
    }

    /**
     * Data class for encrypted data
     */
    data class EncryptedData(
        val cipherText: ByteArray,
        val iv: ByteArray,
        val nonce: ByteArray = ByteArray(0)
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as EncryptedData

            if (!cipherText.contentEquals(other.cipherText)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!nonce.contentEquals(other.nonce)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = cipherText.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + nonce.contentHashCode()
            return result
        }
    }

    /**
     * Data class for authenticated encrypted data
     */
    data class AuthenticatedEncryptedData(
        val cipherText: ByteArray,
        val iv: ByteArray,
        val nonce: ByteArray,
        val hmac: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as AuthenticatedEncryptedData

            if (!cipherText.contentEquals(other.cipherText)) return false
            if (!iv.contentEquals(other.iv)) return false
            if (!nonce.contentEquals(other.nonce)) return false
            if (!hmac.contentEquals(other.hmac)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = cipherText.contentHashCode()
            result = 31 * result + iv.contentHashCode()
            result = 31 * result + nonce.contentHashCode()
            result = 31 * result + hmac.contentHashCode()
            return result
        }
    }
}
