package com.antigravity.virtual32.network

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.graphics.Color
import com.antigravity.virtual32.ui.theme.LedIdle
import com.antigravity.virtual32.ui.theme.LedRedError

/**
 * Encapsulates the visual LED state and haptic feedback pattern for a given response.
 */
data class FeedbackState(
    val ledColor: Color,
    val statusLabel: String,
    val description: String,
    val vibrationPattern: LongArray = longArrayOf()
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FeedbackState
        return ledColor == other.ledColor &&
                statusLabel == other.statusLabel &&
                description == other.description &&
                vibrationPattern.contentEquals(other.vibrationPattern)
    }

    override fun hashCode(): Int {
        var result = ledColor.hashCode()
        result = 31 * result + statusLabel.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + vibrationPattern.contentHashCode()
        return result
    }

    companion object {
        val IDLE = FeedbackState(
            ledColor = LedIdle,
            statusLabel = "IDLE (GRAY)",
            description = "GPIO 2 idle • Ready for trigger",
            vibrationPattern = longArrayOf()
        )
    }
}

/**
 * Single source of truth for all LED state + vibration logic mapped from HTTP responses.
 * Enforces agent.md convention: all response-handling logic lives in one place.
 */
class ResponseStatusHandler(private val context: Context? = null) {

    // Distinct LED color constants for hardware fidelity
    private val colorAmber = Color(0xFFFFB300)     // Timeout / unreachable
    private val colorBlue = Color(0xFF1E88E5)      // No hotspot / disconnected
    private val colorPurple = Color(0xFF8E24AA)    // HTTP 5xx server failure
    private val colorOrange = Color(0xFFFB8C00)    // Malformed body

    /**
     * Maps a NetworkResult to its distinct LED state and vibration pattern,
     * and triggers the device hardware vibration.
     */
    fun processResult(result: NetworkResult): FeedbackState {
        val feedback = when (result) {
            is NetworkResult.Success -> {
                // HTTP 200: stays gray (idle), no vibration
                FeedbackState(
                    ledColor = LedIdle,
                    statusLabel = "HTTP 200 OK (GRAY)",
                    description = "Image received by Phone 1 server.",
                    vibrationPattern = longArrayOf()
                )
            }

            is NetworkResult.ClientError422 -> {
                // HTTP 422: turns red, 1 second vibration
                FeedbackState(
                    ledColor = LedRedError,
                    statusLabel = "HTTP 422 (RED)",
                    description = "Validation error: ${result.message}",
                    vibrationPattern = longArrayOf(0, 1000)
                )
            }

            is NetworkResult.Timeout -> {
                // Request timeout: amber LED, 2 short buzzes
                FeedbackState(
                    ledColor = colorAmber,
                    statusLabel = "TIMEOUT (AMBER)",
                    description = "Request timed out: ${result.message}",
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                )
            }

            is NetworkResult.Unreachable -> {
                // Server unreachable / wrong IP: amber LED, 2 short buzzes
                FeedbackState(
                    ledColor = colorAmber,
                    statusLabel = "UNREACHABLE (AMBER)",
                    description = "Server unreachable: ${result.message}",
                    vibrationPattern = longArrayOf(0, 200, 100, 200)
                )
            }

            is NetworkResult.NoNetwork -> {
                // No network / hotspot down: blue LED, 3 rapid pulses
                FeedbackState(
                    ledColor = colorBlue,
                    statusLabel = "NO NETWORK (BLUE)",
                    description = "Hotspot disconnected or no network route",
                    vibrationPattern = longArrayOf(0, 100, 100, 100, 100, 100)
                )
            }

            is NetworkResult.ServerError5xx -> {
                // HTTP 5xx: purple LED, 1 long + 1 short buzz
                FeedbackState(
                    ledColor = colorPurple,
                    statusLabel = "HTTP ${result.code} (PURPLE)",
                    description = "Phone 1 internal server error: ${result.message}",
                    vibrationPattern = longArrayOf(0, 500, 150, 200)
                )
            }

            is NetworkResult.MalformedResponse -> {
                // Malformed response: orange LED, 1 medium pulse
                FeedbackState(
                    ledColor = colorOrange,
                    statusLabel = "MALFORMED (ORANGE)",
                    description = "Unexpected response body: ${result.message}",
                    vibrationPattern = longArrayOf(0, 400)
                )
            }

            is NetworkResult.UnknownError -> {
                FeedbackState(
                    ledColor = LedRedError,
                    statusLabel = "ERROR (RED)",
                    description = result.message,
                    vibrationPattern = longArrayOf(0, 500)
                )
            }
        }

        executeVibration(feedback.vibrationPattern)
        return feedback
    }

    /**
     * Executes vibration using VibratorManager (API 31+) or Vibrator (API 26+).
     */
    private fun executeVibration(pattern: LongArray) {
        if (pattern.isEmpty()) return
        val ctx = context ?: return

        try {
            val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            if (pattern.size == 2 && pattern[0] == 0L) {
                // Single one-shot pulse
                vibrator.vibrate(
                    VibrationEffect.createOneShot(pattern[1], VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                // Waveform cadence
                vibrator.vibrate(
                    VibrationEffect.createWaveform(pattern, -1)
                )
            }
        } catch (e: Exception) {
            // Silently catch in environments lacking vibrator hardware (e.g. emulators)
        }
    }
}
