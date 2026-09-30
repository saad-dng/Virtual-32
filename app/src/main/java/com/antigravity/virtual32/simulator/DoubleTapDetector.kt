package com.antigravity.virtual32.simulator

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class DoubleTapDetector(
    private val scope: CoroutineScope,
    private val onSingleTap: () -> Unit,
    private val onDoubleTap: () -> Unit
) {
    private var tapJob: Job? = null

    fun onTap() {
        if (tapJob?.isActive == true) {
            // Second tap arrived before the window closed
            tapJob?.cancel()
            tapJob = null
            onDoubleTap()
        } else {
            // First tap
            tapJob = scope.launch {
                delay(BlinkPatterns.DOUBLE_CLICK_WINDOW_MS)
                onSingleTap()
            }
        }
    }
}
