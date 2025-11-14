package com.Mari.mobile

import android.app.Application
import android.util.Base64
import com.Mari.mobileapp.core.security.DeviceKeyManager
import java.net.HttpURLConnection
import java.net.URL
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MariApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Best-effort device registration at process start
        Thread {
            try {
                val prefs = getSharedPreferences("Mari_prefs", MODE_PRIVATE)
                val kid = DeviceKeyManager.getKid()
                val spki = DeviceKeyManager.getPublicKeySpki()
                val spkiB64 = Base64.encodeToString(spki, Base64.NO_WRAP)
                val spkiHash = sha256Hex(spki)
                val prevHash = prefs.getString("spki_hash", null)
                val alreadyRegistered = prefs.getBoolean("device_registered", false)
                val needsRegister = !alreadyRegistered || prevHash == null || prevHash != spkiHash
                if (!needsRegister) return@Thread
                val url = URL("${apiBaseUrl()}/api/transactions/register-device")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val body = "{" +
                        "\"kid\":\"$kid\"," +
                        "\"spki\":\"$spkiB64\"" +
                        "}"
                conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                conn.inputStream.use { it.readBytes() }
                conn.disconnect()
                prefs.edit().putBoolean("device_registered", true).putString("spki_hash", spkiHash).apply()
            } catch (_: Exception) {
                // ignore failures; will retry on next app start
            }
        }.start()
    }

    private fun apiBaseUrl(): String {
        // Prefer Gradle-provided BuildConfig value; optionally overridden via env/system property
        val env = System.getProperty("Mari_API_BASE_URL") ?: System.getenv("Mari_API_BASE_URL")
        val base = env ?: com.Mari.mobile.BuildConfig.CORE_BASE_URL
        return base.trimEnd('/')
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val md = java.security.MessageDigest.getInstance("SHA-256")
        val hash = md.digest(bytes)
        return hash.joinToString("") { String.format("%02x", it) }
    }
}
