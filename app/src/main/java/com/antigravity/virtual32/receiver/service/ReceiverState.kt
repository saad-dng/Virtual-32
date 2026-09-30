package com.antigravity.virtual32.receiver.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.antigravity.virtual32.receiver.pipeline.PipelineState
import com.antigravity.virtual32.data.NextResult

data class BackgroundHealth(
    val isServiceRunning: Boolean = false,
    val hasBatteryExemption: Boolean = false,
    val hasNotificationPermission: Boolean = false,
    val hasWifiLock: Boolean = false,
    val isNetworkUp: Boolean = false,
    val isServerResponding: Boolean = false,
    val currentIp: String? = null,
    val serverPort: Int = 5000,
    val answersCount: Int = 0,
    val lastEspSeenMs: Long = -1L
)

object ReceiverState {
    private val _health = MutableStateFlow(BackgroundHealth())
    val health: StateFlow<BackgroundHealth> = _health
    
    private val _pipelineState = MutableStateFlow<PipelineState?>(null)
    val pipelineState: StateFlow<PipelineState?> = _pipelineState

    private val _lastResult = MutableStateFlow<NextResult?>(null)
    val lastResult: StateFlow<NextResult?> = _lastResult

    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs

    fun update(updater: (BackgroundHealth) -> BackgroundHealth) {
        _health.value = updater(_health.value)
    }

    fun updatePipeline(state: PipelineState) {
        _pipelineState.value = state
    }
    
    fun updateLastResult(res: NextResult) {
        _lastResult.value = res
    }
    
    fun updateLogs(newLogs: List<String>) {
        _logs.value = newLogs
    }
}
