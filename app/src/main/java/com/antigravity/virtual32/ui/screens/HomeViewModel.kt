package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.receiver.pipeline.SessionManager
import com.antigravity.virtual32.receiver.pipeline.SessionUiState
import com.antigravity.virtual32.receiver.service.ReceiverState
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel @JvmOverloads constructor(
    application: Application,
    private val sessionManager: SessionManager = SessionManager.getInstance(application),
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    private var db: AppDatabase? = null
    private var answerStore: RoomAnswerStore? = null

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    val sessionUiState: StateFlow<SessionUiState> = sessionManager.uiState

    init {
        viewModelScope.launch(Dispatchers.IO) {
            val database = AppDatabase.initDatabase(application)
            db = database
            answerStore = RoomAnswerStore(database.answerDao())
            _isLoading.value = false
        }
    }

    fun deletePhoto(index: Int) {
        viewModelScope.launch {
            sessionManager.deletePhoto(index)
        }
    }

    fun moveEarlier(index: Int) {
        viewModelScope.launch {
            sessionManager.moveEarlier(index)
        }
    }

    fun moveLater(index: Int) {
        viewModelScope.launch {
            sessionManager.moveLater(index)
        }
    }

    fun analyzeNow() {
        viewModelScope.launch {
            sessionManager.analyzeNow()
        }
    }

    fun cancelSession() {
        viewModelScope.launch {
            sessionManager.cancelSession()
        }
    }

    fun getAutoSubmitCountdown(nowMs: Long, autoSubmitSec: Int): Int {
        return sessionManager.getAutoSubmitRemainingSec(nowMs, autoSubmitSec)
    }

    fun next() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = answerStore ?: return@launch
            val res = store.next()
            ReceiverState.updateLastResult(res)
        }
    }

    fun repeat() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = answerStore ?: return@launch
            val res = store.repeat()
            ReceiverState.updateLastResult(res)
        }
    }

    fun reset() {
        viewModelScope.launch(Dispatchers.IO) {
            val store = answerStore ?: return@launch
            val res = store.reset()
            ReceiverState.updateLastResult(res)
        }
    }
}
