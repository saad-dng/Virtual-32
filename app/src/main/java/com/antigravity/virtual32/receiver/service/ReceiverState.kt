package com.antigravity.virtual32.receiver.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    fun update(updater: (BackgroundHealth) -> BackgroundHealth) {
        _health.value = updater(_health.value)
    }
}
