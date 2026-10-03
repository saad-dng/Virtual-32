package com.antigravity.virtual32.settings

import kotlinx.serialization.Serializable

enum class AiProvider {
    GEMINI, CLAUDE, NONE
}

enum class AnswerMode {
    REPLACE, APPEND
}

@Serializable
data class AppSettings(
    val serverPort: Int = 5000,
    val provider: AiProvider = AiProvider.GEMINI,
    val fallbackProvider: AiProvider = AiProvider.NONE,
    val geminiModel: String = "gemini-3.8-flash",
    val claudeModel: String = "claude-3-5-sonnet-20241022",
    val geminiKey: String = "",
    val claudeKey: String = "",
    val activePromptId: String = "default",
    val answerMode: AnswerMode = AnswerMode.REPLACE,
    val saveToGallery: Boolean = true,
    val confidenceFlags: Boolean = true,
    val includeReasoning: Boolean = false,
    val pauseAi: Boolean = false,
    val requestTimeoutSec: Int = 40,
    val simHost: String = "127.0.0.1",
    val simPort: Int = 5000,
    val simLoopback: Boolean = false,
    val simResolution: String = "UXGA",
    val simJpegQuality: Int = 80,
    val startOnBoot: Boolean = false,
    val enableHaptics: Boolean = false,
    val maxSessionPages: Int = 12,
    val sessionAutoSubmitSec: Int = 0,
    val multiPhotoInstruction: String = "These N photos are consecutive parts of ONE question paper, in order from the first photo to the last. Read them together as one continuous document. A question may be cut off at the edge of one photo and continue in the next. Neighbouring photos may overlap, so the same question can appear twice: answer each question number once, using the most complete view. Use the printed question numbers.",
    val downscaleSessionPayload: Boolean = true
)
