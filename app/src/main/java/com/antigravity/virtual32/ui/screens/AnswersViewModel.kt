package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.settings.AnswerMode
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import com.antigravity.virtual32.data.AnswerEntity
import com.antigravity.virtual32.data.Batch

data class AnswersUiState(
    val cursor: Int = 0,
    val count: Int = 0,
    val answers: List<AnswerEntity> = emptyList(),
    val answerMode: AnswerMode = AnswerMode.REPLACE,
    val isFilterLowConfidence: Boolean = false,
    val recentBatches: List<Batch> = emptyList()
)

class AnswersViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val answerStore = RoomAnswerStore(db.answerDao())
    private val settingsRepo = SettingsRepository(application)

    private val _isFilterLowConf = MutableStateFlow(false)
    private val _uiState = MutableStateFlow(AnswersUiState())
    val uiState: StateFlow<AnswersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                answerStore.activeAnswers(),
                settingsRepo.settingsFlow,
                _isFilterLowConf
            ) { answersList, settings, filterLow ->
                val cursor = answerStore.cursor
                val batches = db.answerDao().getRecentBatchesFlow() // Need to add this to DAO
                val filtered = if (filterLow) {
                    answersList.filter { it.conf == "low" }
                } else {
                    answersList
                }
                AnswersUiState(
                    cursor = cursor,
                    count = answersList.size,
                    answers = filtered,
                    answerMode = settings.answerMode,
                    isFilterLowConfidence = filterLow,
                    recentBatches = emptyList() // will fill below
                )
            }.collect { state ->
                // fetch batches manually if flow isn't available
                val batches = db.answerDao().getRecentBatches()
                _uiState.value = state.copy(recentBatches = batches)
            }
        }
    }

    fun toggleFilter() {
        _isFilterLowConf.value = !_isFilterLowConf.value
    }

    fun editAnswer(id: Long, newChoice: String) {
        viewModelScope.launch {
            answerStore.editAnswer(id, newChoice)
        }
    }

    fun setCursor(index: Int) {
        viewModelScope.launch {
            answerStore.setCursor(index)
        }
    }

    fun resetCycle() {
        viewModelScope.launch {
            answerStore.reset()
        }
    }

    fun clearList() {
        viewModelScope.launch {
            answerStore.clearActive()
        }
    }
}
