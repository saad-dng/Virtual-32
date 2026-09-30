package com.antigravity.virtual32.settings

import com.antigravity.virtual32.camera.JpegQualityPreset
import com.antigravity.virtual32.camera.Ov3660Resolution

/**
 * Operating mode of the application.
 * Allows a single APK to act as either the Camera Twin rig (Phone 2) or the Receiver Earbud Brain (Phone 1).
 */
enum class AppMode(val label: String) {
    CAMERA_TWIN("Phone 2: ESP32-S3 Camera Twin"),
    RECEIVER_BRAIN("Phone 1: Earbud Brain & Receiver")
}

/**
 * Data model for persisted configuration.
 */
data class AppSettings(
    val serverIp: String = DEFAULT_SERVER_IP,
    val serverPort: Int = DEFAULT_SERVER_PORT,
    val resolution: Ov3660Resolution = Ov3660Resolution.DEFAULT,
    val jpegQuality: Int = JpegQualityPreset.DEFAULT.qualityPercentage,
    val appMode: AppMode = AppMode.CAMERA_TWIN,
    val receiverPort: Int = DEFAULT_RECEIVER_PORT,
    val geminiApiKey: String = "",
    val geminiPrompt: String = DEFAULT_GEMINI_PROMPT,
    val hasCompletedWelcome: Boolean = false
) {
    companion object {
        const val DEFAULT_SERVER_IP = "192.168.43.1"
        const val DEFAULT_SERVER_PORT = 5000
        const val DEFAULT_RECEIVER_PORT = 5000
        const val DEFAULT_GEMINI_PROMPT =
            "You are an earbud voice assistant for a wearable camera. Briefly describe what is in front of the user in 1-2 punchy sentences."
    }
}
