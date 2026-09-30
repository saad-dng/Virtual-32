package com.antigravity.virtual32.receiver.server

import java.util.concurrent.ConcurrentLinkedQueue

class LogBuffer(private val capacity: Int = 500) {
    private val queue = ConcurrentLinkedQueue<String>()

    var lastActivityMs: Long = -1L
        private set

    fun addHttpLog(method: String, path: String, status: Int, durationMs: Long) {
        lastActivityMs = System.currentTimeMillis()
        val logLine = "I/ HTTP $method $path - $status (${durationMs}ms)"
        addRawLog(logLine)
    }

    fun addAppLog(level: String, tag: String, message: String) {
        lastActivityMs = System.currentTimeMillis()
        val logLine = "$level/ $tag: $message"
        addRawLog(logLine)
    }

    private fun addRawLog(logLine: String) {
        queue.add(logLine)
        while (queue.size > capacity) {
            queue.poll()
        }
    }

    fun getLogs(): List<String> = queue.toList()
}
