package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.receiver.service.ReceiverState
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val answerStore = RoomAnswerStore(db.answerDao())

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
