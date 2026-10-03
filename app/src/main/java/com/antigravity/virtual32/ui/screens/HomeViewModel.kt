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
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    application: Application,
    private val sessionManager: SessionManager = SessionManager.getInstance(application),
    private val settingsRepo: SettingsRepository = SettingsRepository(application)
) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val answerStore = RoomAnswerStore(db.answerDao())

    val sessionUiState: StateFlow<SessionUiState> = sessionManager.uiState

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
        viewModelScope.launch {
            val res = answerStore.next()
            ReceiverState.updateLastResult(res)
        }
    }

    fun repeat() {
        viewModelScope.launch {
            val res = answerStore.repeat()
            ReceiverState.updateLastResult(res)
        }
    }

    fun reset() {
        viewModelScope.launch {
            val res = answerStore.reset()
            ReceiverState.updateLastResult(res)
        }
    }
}
