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
    val geminiModel: String = "gemini-1.5-flash",
    val claudeModel: String = "claude-sonnet-5-5",
    val geminiKey: String = "",
    val claudeKey: String = "",
    val activePromptId: String = "default",
    val answerMode: AnswerMode = AnswerMode.REPLACE,
    val saveToGallery: Boolean = true,
    val pauseAi: Boolean = false,
    val requestTimeoutSec: Int = 40,
    val simHost: String = "127.0.0.1",
    val simPort: Int = 5000,
    val simLoopback: Boolean = false,
    val simResolution: String = "UXGA",
    val simJpegQuality: Int = 80
)
