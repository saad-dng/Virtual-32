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

@Serializable
data class Answer(val q: Int, val choice: String, val blinks: Int)

interface AnswerStore {
    val count: Int
    val cursor: Int
    fun next(): NextResult
    fun repeat(): NextResult
    fun reset(): NextResult
    fun replace(newAnswers: List<RawAnswer>)
    fun append(newAnswers: List<RawAnswer>)
}

class InMemoryAnswerStore : AnswerStore {
    private var answers = listOf<Answer>()
    private var _cursor = 0 // 1-based internally, 0 means not started

    override val count: Int get() = answers.size
    override val cursor: Int get() = _cursor

    // Testing helper
    fun setAnswers(newAnswers: List<Answer>) {
        answers = newAnswers
        _cursor = 0
    }

    override fun replace(newAnswers: List<RawAnswer>) {
        answers = newAnswers.map { Answer(it.q, it.choice, confToBlinks(it.choice)) }
        _cursor = 0
    }

    override fun append(newAnswers: List<RawAnswer>) {
        val currentMap = answers.associateBy { it.q }.toMutableMap()
        for (ans in newAnswers) {
            currentMap[ans.q] = Answer(ans.q, ans.choice, confToBlinks(ans.choice))
        }
        answers = currentMap.values.sortedBy { it.q }
    }

    private fun confToBlinks(choice: String): Int {
        return when (choice) {
            "A" -> 1
            "B" -> 2
            "C" -> 3
            "D" -> 4
            "E" -> 5
            else -> 1
        }
    }

    override fun next(): NextResult {
        if (answers.isEmpty()) return NextResult(ok = false, reason = "empty")
        
        if (_cursor >= answers.size) {
            _cursor = answers.size + 1 // mark as end
            return NextResult(ok = true, end = true, of = answers.size)
        }
        
        _cursor++
        val ans = answers[_cursor - 1]
        return NextResult(
            ok = true,
            q = ans.q,
            of = answers.size,
            choice = ans.choice,
            blinks = ans.blinks
        )
    }

    override fun repeat(): NextResult {
        if (answers.isEmpty()) return NextResult(ok = false, reason = "empty")
        if (_cursor == 0 || _cursor > answers.size) return NextResult(ok = false, reason = "empty")
        
        val ans = answers[_cursor - 1]
        return NextResult(
            ok = true,
            q = ans.q,
            of = answers.size,
            choice = ans.choice,
            blinks = ans.blinks
        )
    }

    override fun reset(): NextResult {
        _cursor = 0
        return NextResult(ok = true)
    }
}
