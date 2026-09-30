package com.antigravity.virtual32.receiver.ai

import kotlinx.serialization.Serializable

@Serializable
data class RawAiResult(
    val status: String, // "ok", "unclear", "error"
    val answers: List<RawAnswer> = emptyList(),
    val reason: String? = null,
    val isParseError: Boolean = false
)

@Serializable
data class RawAnswer(
    val q: Int,
    val choice: String, // "A" - "E"
    val conf: String // "high", "low"
)

interface VisionProvider {
    suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult
}

class ProviderException(
    val code: Int,
    val retryAfterSeconds: Int? = null,
    message: String
) : Exception(message)
