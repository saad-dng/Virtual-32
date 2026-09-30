package com.antigravity.virtual32.ui.screens

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.Batch
import com.antigravity.virtual32.receiver.service.ReceiverService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.OutputStream

class HistoryViewModel(private val application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val answerDao = db.answerDao()
    
    val batches = answerDao.getAllBatchesFlow()

    private val _selectedBatches = MutableStateFlow<Set<Long>>(emptySet())
    val selectedBatches: StateFlow<Set<Long>> = _selectedBatches.asStateFlow()

    fun toggleSelection(batchId: Long) {
        val current = _selectedBatches.value.toMutableSet()
        if (current.contains(batchId)) {
            current.remove(batchId)
        } else {
            current.add(batchId)
        }
        _selectedBatches.value = current
    }

    fun clearSelection() {
        _selectedBatches.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            // Delete batches that are NOT in retention. Wait, deleteOldBatches deletes those NOT in retention list.
            // We need a delete query for specific IDs.
            // I'll just write one or use a workaround. Wait, let's update AnswerDao to add deleteBatches(ids: List<Long>)
            val selected = _selectedBatches.value.toList()
            if (selected.isNotEmpty()) {
                answerDao.deleteBatches(selected)
                clearSelection()
            }
        }
    }

    fun reprocess(batch: Batch) {
        if (batch.photoPath != null) {
            val intent = Intent(application, ReceiverService::class.java).apply {
                action = ReceiverService.ACTION_REPROCESS
                putExtra(ReceiverService.EXTRA_PHOTO_PATH, batch.photoPath)
                putExtra(ReceiverService.EXTRA_BATCH_ID, batch.id)
            }
            application.startService(intent)
        }
    }

    suspend fun getCsvContent(): String {
        val selected = _selectedBatches.value.toList()
        if (selected.isEmpty()) return ""
        
        val answers = answerDao.getAnswersForBatches(selected)
        val sb = java.lang.StringBuilder()
        sb.append("BatchID,Q,Choice,Confidence,Edited\n")
        answers.forEach { ans ->
            sb.append("${ans.batchId},${ans.q},${ans.choice},${ans.conf},${ans.edited}\n")
        }
        return sb.toString()
    }
}
