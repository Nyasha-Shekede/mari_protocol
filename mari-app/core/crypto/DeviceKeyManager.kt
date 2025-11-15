package com.Mari.mobileapp.core.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.*
import java.security.spec.ECGenParameterSpec

/**
 * Manages device-specific ECDSA P-256 keys in Android Keystore
 * 
 * The KID (Key Identifier) is the public key that uniquely identifies this device.
 * The private key never leaves the hardware-backed Android Keystore.
 */
class DeviceKeyManager {
    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "mari_device_key"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val EC_CURVE = "secp256r1" // P-256
    }

    private val keyStore: KeyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    /**
     * Initialize or retrieve the device key pair
     * This should be called once during app initialization
     */
    fun initializeDeviceKey(): String {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateDeviceKey()
        }
        return getPublicKeyId()
    }

    /**
     * Generate a new ECDSA P-256 key pair in Android Keystore
     */
    private fun generateDeviceKey() {
        val keyPairGenerator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            KEYSTORE_PROVIDER
        )

        val parameterSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        ).apply {
            setAlgorithmParameterSpec(ECGenParameterSpec(EC_CURVE))
            setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            setUserAuthenticationRequired(false) // Don't require auth for every signature
            setInvalidatedByBiometricEnrollment(false) // Key survives biometric changes
        }.build()

        keyPairGenerator.initialize(parameterSpec)
        keyPairGenerator.generateKeyPair()
    }

    /**
     * Get the public key as a KID (Key Identifier)
     * This is the device's unique identifier
     */
    fun getPublicKeyId(): String {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val publicKey = entry.certificate.publicKey
        
        // Encode public key as Base64 for easy transmission
        val encoded = publicKey.encoded
        return Base64.encodeToString(encoded, Base64.NO_WRAP)
    }

    /**
     * Get the public key in raw bytes
     */
    fun getPublicKeyBytes(): ByteArray {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        return entry.certificate.publicKey.encoded
    }

    /**
     * Sign data with the device's private key
     * The private key never leaves the Android Keystore
     */
    fun signData(data: ByteArray): ByteArray {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
        val privateKey = entry.privateKey

        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(privateKey)
        signature.update(data)
        
        return signature.sign()
    }

    /**
     * Sign data and return Base64-encoded signature
     */
    fun signDataBase64(data: ByteArray): String {
        val signatureBytes = signData(data)
        return Base64.encodeToString(signatureBytes, Base64.NO_WRAP)
    }

    /**
     * Verify a signature using a public key
     */
    fun verifySignature(data: ByteArray, signatureBytes: ByteArray, publicKeyBytes: ByteArray): Boolean {
        return try {
            val keyFactory = KeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_EC)
            val publicKey = keyFactory.generatePublic(
                java.security.spec.X509EncodedKeySpec(publicKeyBytes)
            )

            val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
            signature.initVerify(publicKey)
            signature.update(data)
            signature.verify(signatureBytes)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Check if device key exists
     */
    fun hasDeviceKey(): Boolean {
        return keyStore.containsAlias(KEY_ALIAS)
    }

    /**
     * Delete the device key (use with caution!)
     */
    fun deleteDeviceKey() {
        if (keyStore.containsAlias(KEY_ALIAS)) {
            keyStore.deleteEntry(KEY_ALIAS)
        }
    }

    /**
     * Get a short KID for display (first 16 chars)
     */
    fun getShortKid(): String {
        return getPublicKeyId().take(16)
    }
}
