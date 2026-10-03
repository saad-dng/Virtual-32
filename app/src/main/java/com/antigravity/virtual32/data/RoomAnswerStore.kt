package com.antigravity.virtual32.data

import com.antigravity.virtual32.receiver.ai.RawAnswer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RoomAnswerStore(
    private val dao: AnswerDao
) : AnswerStore {

    private val mutex = Mutex()
    private val _activeAnswersFlow = MutableStateFlow<List<AnswerEntity>>(emptyList())

    suspend fun activeAnswers(): Flow<List<AnswerEntity>> {
        refreshActiveAnswers()
        return _activeAnswersFlow.asStateFlow()
    }

    private suspend fun refreshActiveAnswers() {
        val state = dao.getCycleState() ?: return
        if (state.activeBatchIds.isEmpty()) {
            _activeAnswersFlow.value = emptyList()
            return
        }
        val ids = state.activeBatchIds.split(",").mapNotNull { it.toLongOrNull() }
        val answers = dao.getAnswersForBatches(ids)
        // Deduplicate by q in case of APPEND where newer batches override older ones
        // Since we want newer batches to override, we should sort by batch id (which we assume is increasing)
        // or just rely on the order of ids. Actually, batchId is auto-incrementing, so higher batchId is newer.
        val deduplicated = answers.sortedBy { it.batchId }.associateBy { it.q }.values.sortedBy { it.q }
        _activeAnswersFlow.value = deduplicated.toList()
    }

    suspend fun applyBatch(batch: Batch, answers: List<RawAnswer>, mode: String): Long = mutex.withLock {
        val batchId = dao.insertBatch(batch)
        val entities = answers.map {
            AnswerEntity(
                batchId = batchId,
                q = it.q,
                choice = it.choice,
                conf = it.conf,
                page = it.page
            )
        }
        if (entities.isNotEmpty()) {
            dao.insertAnswers(entities)
        }

        var state = dao.getCycleState()
        if (state == null) {
            state = CycleState(id = 1, activeBatchIds = "", cursor = 0)
            dao.insertCycleState(state)
        }

        if (mode == "REPLACE") {
            val oldIds = state.activeBatchIds.split(",").mapNotNull { it.toLongOrNull() }
            if (oldIds.isNotEmpty()) {
                dao.markBatchesSuperseded(oldIds)
            }
            state = state.copy(activeBatchIds = batchId.toString(), cursor = 0)
        } else {
            // APPEND
            val newIds = if (state.activeBatchIds.isEmpty()) batchId.toString() else "${state.activeBatchIds},$batchId"
            state = state.copy(activeBatchIds = newIds) // keep cursor
        }
        
        dao.updateCycleState(state)
        refreshActiveAnswers()
        
        // Handle retention
        val latestIds = dao.getLatest100BatchIds()
        dao.deleteOldBatches(latestIds)

        return@withLock batchId
    }

    suspend fun editAnswer(answerId: Long, newChoice: String) = mutex.withLock {
        dao.editAnswerChoice(answerId, newChoice)
        refreshActiveAnswers()
    }

    suspend fun clearActive() = mutex.withLock {
        val state = dao.getCycleState()
        if (state != null) {
            val oldIds = state.activeBatchIds.split(",").mapNotNull { it.toLongOrNull() }
            if (oldIds.isNotEmpty()) {
                dao.markBatchesSuperseded(oldIds)
            }
            dao.updateCycleState(state.copy(activeBatchIds = "", cursor = 0))
            refreshActiveAnswers()
        }
    }

    override fun next(): NextResult {
        // AnswerStore interface methods are synchronous, we'll need to runBlocking or refactor interface
        // The prompt says "Real AnswerStore over Room: next(), repeat(), reset(), setCursor(i)..."
        // Wait, AnswerStore interface has fun next(): NextResult. Since room is suspend, we might need runBlocking here.
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                val state = dao.getCycleState() ?: return@withLock NextResult(ok = false, reason = "empty")
                val list = _activeAnswersFlow.value
                if (list.isEmpty()) return@withLock NextResult(ok = false, reason = "empty")
                
                if (state.cursor >= list.size) {
                    dao.updateCycleState(state.copy(cursor = 0))
                    return@withLock NextResult(ok = false, reason = "wrap", end = true)
                }

                val ans = list[state.cursor]
                val newState = state.copy(cursor = state.cursor + 1)
                dao.updateCycleState(newState)
                
                NextResult(
                    ok = true,
                    q = ans.q,
                    of = list.last().q,
                    choice = ans.choice,
                    blinks = confToBlinks(ans.choice)
                )
            }
        }
    }

    override fun repeat(): NextResult {
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                val state = dao.getCycleState() ?: return@withLock NextResult(ok = false, reason = "empty")
                val list = _activeAnswersFlow.value
                if (list.isEmpty()) return@withLock NextResult(ok = false, reason = "empty")
                
                var c = state.cursor - 1
                if (c < 0) c = 0
                if (c >= list.size) c = list.size - 1
                
                val ans = list[c]
                
                NextResult(
                    ok = true,
                    q = ans.q,
                    of = list.last().q,
                    choice = ans.choice,
                    blinks = confToBlinks(ans.choice)
                )
            }
        }
    }

    override fun reset(): NextResult {
        return kotlinx.coroutines.runBlocking {
            mutex.withLock {
                val state = dao.getCycleState() ?: return@withLock NextResult(ok = false, reason = "empty")
                val list = _activeAnswersFlow.value
                if (list.isEmpty()) return@withLock NextResult(ok = false, reason = "empty")
                
                dao.updateCycleState(state.copy(cursor = 0))
                
                NextResult(ok = true, reason = "reset")
            }
        }
    }

    suspend fun setCursor(i: Int) = mutex.withLock {
        val state = dao.getCycleState()
        if (state != null) {
            dao.updateCycleState(state.copy(cursor = i))
        }
    }

    override val cursor: Int
        get() = kotlinx.coroutines.runBlocking {
            dao.getCycleState()?.cursor ?: 0
        }

    override val count: Int
        get() = _activeAnswersFlow.value.size

    override fun replace(newAnswers: List<RawAnswer>) {
        throw UnsupportedOperationException("Use applyBatch instead")
    }

    override fun append(newAnswers: List<RawAnswer>) {
        throw UnsupportedOperationException("Use applyBatch instead")
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
}
