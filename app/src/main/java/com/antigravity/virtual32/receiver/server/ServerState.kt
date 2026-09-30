package com.antigravity.virtual32.receiver.server

data class ServerState(
    val isRunning: Boolean = false,
    val boundIp: String = "127.0.0.1",
    val port: Int = 5000,
    val lastSeenMs: Long = 0L,
    val requestCount: Int = 0,
    val lastError: String? = null
)
