package com.Mari.mobileapp.core.security

import android.os.Build
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.*
import java.security.cert.CertificateFactory
import java.security.interfaces.ECPublicKey
import java.security.spec.X509EncodedKeySpec
import java.security.spec.AlgorithmParameterSpec
import javax.security.auth.x500.X500Principal

class DeviceKeyManager private constructor() {
    companion object {
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MariDeviceKey"
        private var initialized = false

        private fun ensureKeyGenerated() {
            if (initialized) return
            synchronized(this) {
                if (initialized) return
                val ks = KeyStore.getInstance(ANDROID_KEY_STORE)
                ks.load(null)
                if (!ks.containsAlias(KEY_ALIAS)) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        val kpg = KeyPairGenerator.getInstance(
                            KeyProperties.KEY_ALGORITHM_EC,
                            ANDROID_KEY_STORE
                        )
                        val parameterSpec: AlgorithmParameterSpec = KeyGenParameterSpec.Builder(
                            KEY_ALIAS,
                            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
                        )
                            .setAlgorithmParameterSpec(java.security.spec.ECGenParameterSpec("secp256r1"))
                            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA384, KeyProperties.DIGEST_SHA512)
                            .setUserAuthenticationRequired(false)
                            .build()
                        kpg.initialize(parameterSpec)
                        kpg.generateKeyPair()
                    } else {
                        // Fallback for <23 not supported in this demo
                        throw RuntimeException("Android < 23 not supported for device signing demo")
                    }
                }
                initialized = true
            }
        }

        fun getPublicKeySpki(): ByteArray {
            ensureKeyGenerated()
            val ks = KeyStore.getInstance(ANDROID_KEY_STORE)
            ks.load(null)
            val cert = ks.getCertificate(KEY_ALIAS)
            return cert.publicKey.encoded // X.509 SubjectPublicKeyInfo
        }

        fun getKid(): String {
            val spki = getPublicKeySpki()
            val md = MessageDigest.getInstance("SHA-256")
            val hash = md.digest(spki)
            // First 8 hex chars
            return hash.take(4).joinToString("") { String.format("%02x", it) }
        }

        fun signSha256Ecdsa(data: ByteArray): ByteArray {
            ensureKeyGenerated()
            val ks = KeyStore.getInstance(ANDROID_KEY_STORE)
            ks.load(null)
            val entry = ks.getEntry(KEY_ALIAS, null) as KeyStore.PrivateKeyEntry
            val privateKey = entry.privateKey
            val sig = Signature.getInstance("SHA256withECDSA")
            sig.initSign(privateKey)
            sig.update(data)
            return sig.sign() // DER-encoded ECDSA signature
        }

        fun signToBase64Url(data: ByteArray): String {
            val der = signSha256Ecdsa(data)
            return Base64.encodeToString(der, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
        }
    }
}
