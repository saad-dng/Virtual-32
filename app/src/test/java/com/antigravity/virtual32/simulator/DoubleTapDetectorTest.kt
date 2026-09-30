package com.antigravity.virtual32.simulator

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DoubleTapDetectorTest {

    @Test
    fun `single tap fires after window`() = runTest {
        var singleFired = 0
        var doubleFired = 0
        val detector = DoubleTapDetector(
            scope = this,
            onSingleTap = { singleFired++ },
            onDoubleTap = { doubleFired++ }
        )

        detector.onTap()
        assertEquals(0, singleFired)
        assertEquals(0, doubleFired)

        advanceTimeBy(351) // Past the window
        assertEquals(1, singleFired)
        assertEquals(0, doubleFired)
    }

    @Test
    fun `double tap fires immediately when second tap arrives within window`() = runTest {
        var singleFired = 0
        var doubleFired = 0
        val detector = DoubleTapDetector(
            scope = this,
            onSingleTap = { singleFired++ },
            onDoubleTap = { doubleFired++ }
        )

        detector.onTap()
        advanceTimeBy(100) // Within window
        detector.onTap()
        
        assertEquals(0, singleFired)
        assertEquals(1, doubleFired)
        
        advanceTimeBy(300) // Wait out the rest of the original window
        assertEquals(0, singleFired) // Should have been cancelled
    }

    @Test
    fun `slow double tap is two single taps`() = runTest {
        var singleFired = 0
        var doubleFired = 0
        val detector = DoubleTapDetector(
            scope = this,
            onSingleTap = { singleFired++ },
            onDoubleTap = { doubleFired++ }
        )

        detector.onTap()
        advanceTimeBy(400) // Past the window
        assertEquals(1, singleFired)
        
        detector.onTap()
        advanceTimeBy(400) // Past the window
        assertEquals(2, singleFired)
        assertEquals(0, doubleFired)
    }

    @Test
    fun `triple tap within window triggers double then single`() = runTest {
        var singleFired = 0
        var doubleFired = 0
        val detector = DoubleTapDetector(
            scope = this,
            onSingleTap = { singleFired++ },
            onDoubleTap = { doubleFired++ }
        )

        detector.onTap()
        advanceTimeBy(100)
        detector.onTap() // Double tap triggers here
        assertEquals(1, doubleFired)
        assertEquals(0, singleFired)

        advanceTimeBy(10)
        detector.onTap() // Third tap starts a new window
        advanceTimeBy(400) // Third tap window expires
        assertEquals(1, doubleFired)
        assertEquals(1, singleFired)
    }
}
