package com.antigravity.virtual32.data

import kotlinx.serialization.Serializable
import com.antigravity.virtual32.receiver.ai.RawAnswer

@Serializable
data class NextResult(
    val ok: Boolean,
    val reason: String? = null,
    val q: Int? = null,
    val of: Int? = null,
    val choice: String? = null,
    val blinks: Int? = null,
    val end: Boolean? = null
)

interface AnswerStore {
    val cursor: Int
    val count: Int
    fun next(): NextResult
    fun repeat(): NextResult
    fun reset(): NextResult
    fun replace(newAnswers: List<RawAnswer>)
    fun append(newAnswers: List<RawAnswer>)
}
