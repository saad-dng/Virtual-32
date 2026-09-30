package com.antigravity.virtual32.receiver.server

import java.util.concurrent.ConcurrentLinkedQueue

class LogBuffer(private val capacity: Int = 500) {
    private val queue = ConcurrentLinkedQueue<String>()

    fun addLog(method: String, path: String, status: Int, durationMs: Long) {
        val logLine = "$method $path - $status (${durationMs}ms)"
        queue.add(logLine)
        while (queue.size > capacity) {
            queue.poll()
        }
    }

    fun getLogs(): List<String> = queue.toList()
}
