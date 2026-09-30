package com.antigravity.virtual32.camera

import android.util.Size

/**
 * Specifications and presets for the OmniVision OV3660 camera sensor,
 * matching real-world ESP32-S3-CAM hardware parity.
 */
enum class Ov3660Resolution(
    val label: String,
    val width: Int,
    val height: Int,
    val description: String,
    val typicalPayloadKb: String
) {
    VGA("VGA (640x480)", 640, 480, "Fast streaming / low Wi-Fi latency", "~35 KB"),
    SVGA("SVGA (800x600)", 800, 600, "ESP32-S3 standard default • Balanced", "~65 KB"),
    XGA("XGA (1024x768)", 1024, 768, "Moderate detail • Good for scene analysis", "~95 KB"),
    UXGA("UXGA (1600x1200)", 1600, 1200, "High detail • Ideal for text/OCR", "~180 KB"),
    QXGA("QXGA (2048x1536)", 2048, 1536, "Full 3MP native sensor • Higher transfer latency", "~350 KB");

    val size: Size get() = Size(width, height)
    val aspectRatio: Float get() = 4f / 3f

    companion object {
        val DEFAULT = SVGA

        fun fromLabel(label: String): Ov3660Resolution {
            return entries.firstOrNull { it.name.equals(label, ignoreCase = true) || it.label.startsWith(label) } ?: DEFAULT
        }
    }
}

/**
 * Tunable JPEG compression quality presets matching real ESP32-S3-CAM DMA buffers.
 */
enum class JpegQualityPreset(
    val label: String,
    val qualityPercentage: Int,
    val description: String
) {
    FAST("Fast (65%)", 65, "Low payload • Minimum Wi-Fi transfer time"),
    BALANCED("Balanced (80%)", 80, "ESP32-S3-CAM sweet spot • Clean fidelity"),
    FINE("Fine (92%)", 92, "Maximum quality • Near lossless capture");

    companion object {
        val DEFAULT = BALANCED

        fun fromQuality(percentage: Int): JpegQualityPreset {
            return entries.minByOrNull { kotlin.math.abs(it.qualityPercentage - percentage) } ?: DEFAULT
        }

        fun fromName(name: String): JpegQualityPreset {
            return entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: DEFAULT
        }
    }
}

object SensorSpecs {
    const val SENSOR_NAME = "OmniVision OV3660"
    const val MAX_RESOLUTION = "2048x1536 (3MP QXGA)"
    const val DEFAULT_FOV_DIAGONAL = 66.5f // ~65-68 deg physical lens
    const val ASPECT_RATIO_NUMERATOR = 4
    const val ASPECT_RATIO_DENOMINATOR = 3
    const val ASPECT_RATIO_FLOAT = 4f / 3f

    // Real hardware references for ESP32-S3 + OV3660
    const val HARDWARE_PLATFORM = "ESP32-S3 (8MB Octal PSRAM)"
    const val TYPICAL_DMA_LIMIT_KB = 512
}
