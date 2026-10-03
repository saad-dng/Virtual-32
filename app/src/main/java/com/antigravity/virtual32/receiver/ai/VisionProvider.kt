package com.antigravity.virtual32.receiver.ai

import kotlinx.serialization.Serializable

@Serializable
data class RawAiResult(
    val status: String, // "ok", "unclear", "error"
    val answers: List<RawAnswer> = emptyList(),
    val reason: String? = null,
    val isParseError: Boolean = false,
    val unreadablePhotos: List<Int> = emptyList(),
    val warnings: List<String> = emptyList()
)

@Serializable
data class RawAnswer(
    val q: Int,
    val choice: String, // "A" - "E"
    val conf: String, // "high", "low"
    val reasoning: String? = null,
    val page: Int = 1
)

interface VisionProvider {
    suspend fun analyze(jpegs: List<ByteArray>, instruction: String): RawAiResult =
        analyze(jpegs.firstOrNull() ?: ByteArray(0), instruction)

    suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult =
        analyze(listOf(jpeg), instruction)
}

class ProviderException(
    val code: Int,
    val retryAfterSeconds: Int? = null,
    message: String
) : Exception(message)
