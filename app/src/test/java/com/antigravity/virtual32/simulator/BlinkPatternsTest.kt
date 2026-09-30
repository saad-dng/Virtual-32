package com.antigravity.virtual32.simulator

import org.junit.Assert.assertEquals
import org.junit.Test

class BlinkPatternsTest {

    @Test
    fun `answerBlinks generates correct number of blinks`() {
        val pattern = BlinkPatterns.answerBlinks(3)
        assertEquals(6, pattern.size) // 3 on + 3 off
        assertEquals(true, pattern[0].on)
        assertEquals(250L, pattern[0].durationMs)
        assertEquals(false, pattern[1].on)
        assertEquals(250L, pattern[1].durationMs)
    }

    @Test
    fun `processingPulse is correct`() {
        assertEquals(2, BlinkPatterns.processingPulse.size)
        assertEquals(BlinkStep(true, 500L), BlinkPatterns.processingPulse[0])
        assertEquals(BlinkStep(false, 500L), BlinkPatterns.processingPulse[1])
    }

    @Test
    fun `serverError has 5 fast blinks`() {
        val pattern = BlinkPatterns.serverError
        assertEquals(10, pattern.size)
        assertEquals(BlinkStep(true, 120L), pattern[0])
    }

    @Test
    fun `cycleComplete has 2 medium blinks`() {
        val pattern = BlinkPatterns.cycleComplete
        assertEquals(4, pattern.size)
        assertEquals(BlinkStep(true, 500L), pattern[0])
        assertEquals(BlinkStep(false, 300L), pattern[1])
        assertEquals(BlinkStep(true, 500L), pattern[2])
    }
}
