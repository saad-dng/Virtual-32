package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.data.AnswerDao
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.AnswerEntity
import com.antigravity.virtual32.data.Batch
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.settings.AnswerMode
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class AnswersUiState(
    val cursor: Int = 0,
    val count: Int = 0,
    val answers: List<AnswerEntity> = emptyList(),
    val answerMode: AnswerMode = AnswerMode.REPLACE,
    val isFilterLowConfidence: Boolean = false,
    val selectedPhotoFilter: Int? = null,
    val availablePages: List<Int> = emptyList(),
    val warnings: List<String> = emptyList(),
    val warningHint: String? = null,
    val isWarningsDismissed: Boolean = false,
    val recentBatches: List<Batch> = emptyList()
)

class AnswersViewModel @JvmOverloads constructor(
    application: Application,
    private val answerStore: RoomAnswerStore = RoomAnswerStore(AppDatabase.getDatabase(application).answerDao()),
    private val settingsRepo: SettingsRepository = SettingsRepository(application),
    private val answerDao: AnswerDao = AppDatabase.getDatabase(application).answerDao()
) : AndroidViewModel(application) {

    private val _isFilterLowConf = MutableStateFlow(false)
    private val _selectedPhotoFilter = MutableStateFlow<Int?>(null)
    private val _isWarningsDismissed = MutableStateFlow(false)
    private var lastBatchId: Long = -1L

    private val _uiState = MutableStateFlow(AnswersUiState())
    val uiState: StateFlow<AnswersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                answerStore.activeAnswers(),
                settingsRepo.settingsFlow,
                _isFilterLowConf,
                _selectedPhotoFilter,
                _isWarningsDismissed
            ) { answersList, settings, filterLow, photoFilter, dismissed ->
                val cursor = answerStore.cursor
                val batches = answerDao.getRecentBatches()
                val latestBatch = batches.firstOrNull()

                if (latestBatch != null && latestBatch.id != lastBatchId) {
                    lastBatchId = latestBatch.id
                    _isWarningsDismissed.value = false
                }

                val pages = answersList.map { it.page }.distinct().sorted()
                val warnings = latestBatch?.getWarningList() ?: emptyList()
                val hint = computeWarningHint(warnings)

                var filtered = answersList
                if (filterLow) {
                    filtered = filtered.filter { it.conf == "low" }
                }
                if (photoFilter != null) {
                    filtered = filtered.filter { it.page == photoFilter }
                }

                AnswersUiState(
                    cursor = cursor,
                    count = answersList.size,
                    answers = filtered,
                    answerMode = settings.answerMode,
                    isFilterLowConfidence = filterLow,
                    selectedPhotoFilter = photoFilter,
                    availablePages = pages,
                    warnings = warnings,
                    warningHint = hint,
                    isWarningsDismissed = dismissed,
                    recentBatches = batches
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun toggleFilter() {
        _isFilterLowConf.value = !_isFilterLowConf.value
    }

    fun setPhotoFilter(page: Int?) {
        _selectedPhotoFilter.value = page
    }

    fun dismissWarnings() {
        _isWarningsDismissed.value = true
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

    companion object {
        fun computeWarningHint(warnings: List<String>): String? {
            if (warnings.isEmpty()) return null
            val unreadable = warnings.firstOrNull { it.contains("unreadable", ignoreCase = true) }
            if (unreadable != null) {
                val match = Regex("""Photo\s+(\d+)""", RegexOption.IGNORE_CASE).find(unreadable)
                val photoNum = match?.groupValues?.get(1)
                return if (photoNum != null) {
                    "retake photo $photoNum and send all again"
                } else {
                    "retake unreadable photo and send all again"
                }
            }
            val missing = warnings.firstOrNull { it.contains("missing", ignoreCase = true) }
            if (missing != null) {
                return "retake overlapping pages and send all again"
            }
            return "retake photos and send all again"
        }
    }
}
