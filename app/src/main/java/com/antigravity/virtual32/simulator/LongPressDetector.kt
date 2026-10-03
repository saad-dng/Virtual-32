package com.antigravity.virtual32.simulator

class LongPressDetector(
    private val thresholdMs: Long = BlinkPatterns.LONG_PRESS_MS,
    private val touchSlopPx: Float = 40f,
    private val timeSource: () -> Long = { System.currentTimeMillis() },
    private val onShortPress: (() -> Unit)? = null,
    private val onLongPress: (() -> Unit)? = null
) {
    enum class Result {
        NONE,
        SHORT_PRESS,
        LONG_PRESS,
        CANCELLED
    }

    private var downTimeMs: Long = 0L
    private var downX: Float = 0f
    private var downY: Float = 0f
    private var isPressed: Boolean = false
    private var isCancelled: Boolean = false

    fun onDown(x: Float = 0f, y: Float = 0f, timeMs: Long = timeSource()) {
        downTimeMs = timeMs
        downX = x
        downY = y
        isPressed = true
        isCancelled = false
    }

    fun onMove(x: Float, y: Float): Boolean {
        if (!isPressed || isCancelled) return false
        val dx = x - downX
        val dy = y - downY
        val distSq = dx * dx + dy * dy
        if (distSq > touchSlopPx * touchSlopPx) {
            isCancelled = true
            return true
        }
        return false
    }

    fun onCancel() {
        isPressed = false
        isCancelled = true
    }

    fun onUp(x: Float = downX, y: Float = downY, timeMs: Long = timeSource()): Result {
        if (!isPressed) return Result.NONE
        isPressed = false
        if (isCancelled || onMove(x, y)) {
            return Result.CANCELLED
        }

        val duration = timeMs - downTimeMs
        return if (duration >= thresholdMs) {
            onLongPress?.invoke()
            Result.LONG_PRESS
        } else {
            onShortPress?.invoke()
            Result.SHORT_PRESS
        }
    }
}
