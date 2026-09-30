package com.antigravity.virtual32.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface AnswerDao {
    
    @Insert
    suspend fun insertBatch(batch: Batch): Long

    @Insert
    suspend fun insertAnswers(answers: List<AnswerEntity>)

    @Query("UPDATE batches SET superseded = 1 WHERE id IN (:batchIds)")
    suspend fun markBatchesSuperseded(batchIds: List<Long>)

    @Query("SELECT * FROM cycle_state WHERE id = 1")
    suspend fun getCycleState(): CycleState?

    @Insert
    suspend fun insertCycleState(state: CycleState)

    @Update
    suspend fun updateCycleState(state: CycleState)

    @Query("SELECT * FROM answers WHERE batchId IN (:batchIds) ORDER BY q ASC")
    suspend fun getAnswersForBatches(batchIds: List<Long>): List<AnswerEntity>
    
    @Query("SELECT * FROM answers WHERE batchId IN (:batchIds) ORDER BY q ASC")
    fun getAnswersForBatchesFlow(batchIds: List<Long>): Flow<List<AnswerEntity>>

    @Query("UPDATE answers SET choice = :newChoice, edited = 1 WHERE id = :answerId")
    suspend fun editAnswerChoice(answerId: Long, newChoice: String)

    @Query("SELECT id FROM batches ORDER BY id DESC LIMIT 100")
    suspend fun getLatest100BatchIds(): List<Long>

    @Query("DELETE FROM batches WHERE id NOT IN (:retentionIds)")
    suspend fun deleteOldBatches(retentionIds: List<Long>)

    @Query("SELECT * FROM batches ORDER BY id DESC LIMIT 10")
    suspend fun getRecentBatches(): List<Batch>
    
    @Query("SELECT * FROM batches ORDER BY id DESC LIMIT 10")
    fun getRecentBatchesFlow(): Flow<List<Batch>>

    @Query("SELECT * FROM batches ORDER BY id DESC")
    fun getAllBatchesFlow(): Flow<List<Batch>>

    @Query("SELECT COUNT(*) FROM batches WHERE createdAt >= :startTime")
    suspend fun getPhotosSince(startTime: Long): Int

    @Query("SELECT COUNT(*) FROM batches WHERE status = :status")
    suspend fun getCountByStatus(status: String): Int

    @Query("SELECT AVG(latencyMs) FROM batches WHERE status = 'ok' AND latencyMs > 0")
    suspend fun getAverageLatency(): Long?

    @Query("SELECT COUNT(*) FROM batches WHERE provider = :provider")
    suspend fun getCountByProvider(provider: String): Int

    @Query("SELECT COUNT(*) FROM batches WHERE rawResponse LIKE '%429%' OR rawResponse LIKE '%rate limit%'")
    suspend fun getRateLimitCount(): Int
    
    @Query("DELETE FROM batches WHERE id IN (:batchIds)")
    suspend fun deleteBatches(batchIds: List<Long>)
}
