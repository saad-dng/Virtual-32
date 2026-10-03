package com.antigravity.virtual32.simulator

import org.junit.Assert.*
import org.junit.Test

class LongPressDetectorTest {

    @Test
    fun testShortPress() {
        var currentTime = 1000L
        var shortInvoked = false
        var longInvoked = false

        val detector = LongPressDetector(
            thresholdMs = 1500L,
            touchSlopPx = 40f,
            timeSource = { currentTime },
            onShortPress = { shortInvoked = true },
            onLongPress = { longInvoked = true }
        )

        detector.onDown(x = 100f, y = 100f, timeMs = currentTime)
        currentTime += 400L // 400 ms < 1500 ms
        val result = detector.onUp(x = 100f, y = 100f, timeMs = currentTime)

        assertEquals(LongPressDetector.Result.SHORT_PRESS, result)
        assertTrue("Short press callback should be invoked", shortInvoked)
        assertFalse("Long press callback should not be invoked", longInvoked)
    }

    @Test
    fun testExactlyAtTheLimit() {
        var currentTime = 1000L
        var shortInvoked = false
        var longInvoked = false

        val detector = LongPressDetector(
            thresholdMs = 1500L,
            touchSlopPx = 40f,
            timeSource = { currentTime },
            onShortPress = { shortInvoked = true },
            onLongPress = { longInvoked = true }
        )

        detector.onDown(x = 100f, y = 100f, timeMs = currentTime)
        currentTime += 1500L // Exactly 1500 ms
        val result = detector.onUp(x = 100f, y = 100f, timeMs = currentTime)

        assertEquals(LongPressDetector.Result.LONG_PRESS, result)
        assertFalse("Short press callback should not be invoked", shortInvoked)
        assertTrue("Long press callback should be invoked", longInvoked)
    }

    @Test
    fun testLongPress() {
        var currentTime = 1000L
        var shortInvoked = false
        var longInvoked = false

        val detector = LongPressDetector(
            thresholdMs = 1500L,
            touchSlopPx = 40f,
            timeSource = { currentTime },
            onShortPress = { shortInvoked = true },
            onLongPress = { longInvoked = true }
        )

        detector.onDown(x = 100f, y = 100f, timeMs = currentTime)
        currentTime += 2200L // 2200 ms > 1500 ms
        val result = detector.onUp(x = 100f, y = 100f, timeMs = currentTime)

        assertEquals(LongPressDetector.Result.LONG_PRESS, result)
        assertFalse("Short press callback should not be invoked", shortInvoked)
        assertTrue("Long press callback should be invoked", longInvoked)
    }

    @Test
    fun testCancelledByMovingAway() {
        var currentTime = 1000L
        var shortInvoked = false
        var longInvoked = false

        val detector = LongPressDetector(
            thresholdMs = 1500L,
            touchSlopPx = 40f,
            timeSource = { currentTime },
            onShortPress = { shortInvoked = true },
            onLongPress = { longInvoked = true }
        )

        detector.onDown(x = 100f, y = 100f, timeMs = currentTime)
        currentTime += 500L
        val moved = detector.onMove(x = 160f, y = 100f) // moved 60px > 40px
        assertTrue("Movement beyond slop should cancel", moved)

        currentTime += 1200L
        val result = detector.onUp(x = 160f, y = 100f, timeMs = currentTime)

        assertEquals(LongPressDetector.Result.CANCELLED, result)
        assertFalse("Short press callback should not be invoked on cancel", shortInvoked)
        assertFalse("Long press callback should not be invoked on cancel", longInvoked)
    }
}
