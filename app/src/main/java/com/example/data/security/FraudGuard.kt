package com.example.data.security

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import java.io.File
import java.security.MessageDigest

data class FraudAssessment(
    val deviceHash: String,
    val riskScore: Int,
    val riskLevel: String,
    val signals: List<String>,
    val isVpnActive: Boolean,
    val isRooted: Boolean,
    val isEmulator: Boolean,
    val blockActivity: Boolean
)

class FraudGuard(private val context: Context) {

    private var lastActionElapsedMs: Long = 0L
    private var rapidTapCount: Int = 0

    @SuppressLint("HardwareIds")
    fun computeDeviceHash(): String {
        val rawId = try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                ?: "${Build.BOARD}-${Build.BRAND}-${Build.DEVICE}"
        } catch (_: Exception) {
            "${Build.BOARD}-${Build.BRAND}-${Build.DEVICE}"
        }
        val digest = MessageDigest.getInstance("SHA-256").digest("RP_SALT:$rawId".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }.take(32)
    }

    fun assessEnvironment(sessionStartedAtMs: Long = 0L, requiredDwellMs: Long = 0L): FraudAssessment {
        val signals = mutableListOf<String>()
        var score = 5 // Baseline low risk

        val rooted = checkRootIndicators()
        if (rooted) {
            score += 45
            signals += "Rooted binary / su indicator detected"
        }

        val vpnActive = checkVpnActive()
        if (vpnActive) {
            score += 30
            signals += "Active VPN/Proxy tunnel detected"
        }

        val emulator = checkEmulatorEnvironment()
        if (emulator) {
            score += 10
            signals += "Virtualized / Emulator test runtime"
        }

        val nowElapsed = SystemClock.elapsedRealtime()
        if (sessionStartedAtMs > 0L && requiredDwellMs > 0L) {
            val dwell = nowElapsed - sessionStartedAtMs
            if (dwell < requiredDwellMs) {
                score += 40
                signals += "Abnormal fast-completion velocity (${dwell}ms < ${requiredDwellMs}ms)"
            }
        }

        if (lastActionElapsedMs > 0L && (nowElapsed - lastActionElapsedMs) < 3500L) {
            rapidTapCount += 1
            if (rapidTapCount >= 2) {
                score += 25
                signals += "Rapid repeated claim automation pattern"
            }
        } else {
            rapidTapCount = 0
        }

        val clampedScore = score.coerceIn(0, 100)
        val riskLevel = when {
            clampedScore >= 75 -> "CRITICAL"
            clampedScore >= 50 -> "HIGH"
            clampedScore >= 25 -> "MEDIUM"
            else -> "LOW"
        }
        // Block automated/rooted/VPN abuse on real payouts/claims while allowing standard debug/emulator testing
        val shouldBlock = rooted || (vpnActive && clampedScore >= 60) || clampedScore >= 80

        return FraudAssessment(
            deviceHash = computeDeviceHash(),
            riskScore = clampedScore,
            riskLevel = riskLevel,
            signals = if (signals.isEmpty()) listOf("Device integrity & network verified") else signals,
            isVpnActive = vpnActive,
            isRooted = rooted,
            isEmulator = emulator,
            blockActivity = shouldBlock
        )
    }

    fun recordActionTimestamp() {
        lastActionElapsedMs = SystemClock.elapsedRealtime()
    }

    private fun checkRootIndicators(): Boolean {
        val suPaths = listOf(
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        )
        if (suPaths.any { path -> try { File(path).exists() } catch (_: Exception) { false } }) {
            return true
        }
        val tags = Build.TAGS
        return tags != null && tags.contains("test-keys") && !checkEmulatorEnvironment()
    }

    private fun checkVpnActive(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        } catch (_: Exception) {
            false
        }
    }

    private fun checkEmulatorEnvironment(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
            Build.FINGERPRINT.startsWith("generic") ||
            Build.FINGERPRINT.startsWith("unknown") ||
            Build.HARDWARE.contains("goldfish") ||
            Build.HARDWARE.contains("ranchu") ||
            Build.MODEL.contains("google_sdk") ||
            Build.MODEL.contains("Emulator") ||
            Build.MODEL.contains("Android SDK built for")
    }
}
