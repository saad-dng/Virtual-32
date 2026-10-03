package com.antigravity.virtual32.simulator

/**
 * Defines a single state in a blink sequence.
 * @param on True if the LED should be on.
 * @param durationMs Duration of this state in milliseconds.
 */
data class BlinkStep(val on: Boolean, val durationMs: Long)

/**
 * Blink patterns defined exactly as per guide.md §4.
 * The single source of truth for blink timings.
 */
object BlinkPatterns {
    val DOUBLE_CLICK_WINDOW_MS = 350L
    const val LONG_PRESS_MS = 1500L

    // Photo added to session: blue 1 flash of 100 ms (shorter than an answer blink, which is 250 ms)
    val PHOTO_ADDED = listOf(
        BlinkStep(true, 100L),
        BlinkStep(false, 0L)
    )

    // Answer: N blinks (250 ms on / 250 ms off)
    fun answerBlinks(n: Int): List<BlinkStep> {
        val steps = mutableListOf<BlinkStep>()
        for (i in 0 until n) {
            steps.add(BlinkStep(true, 250L))
            steps.add(BlinkStep(false, 250L))
        }
        return steps
    }

    // Processing: slow pulse (500 on / 500 off) until a reply
    val processingPulse = listOf(
        BlinkStep(true, 500L),
        BlinkStep(false, 500L)
    )

    // Ready (upload ok): solid 1000 ms
    val readyUploadOk = listOf(
        BlinkStep(true, 1000L),
        BlinkStep(false, 0L)
    )

    // Red LED patterns:

    // Photo unclear: 1 long (1200 ms)
    val photoUnclear = listOf(
        BlinkStep(true, 1200L),
        BlinkStep(false, 0L)
    )

    // Cycle complete: 2 medium (500 on / 300 off)
    val cycleComplete = listOf(
        BlinkStep(true, 500L),
        BlinkStep(false, 300L),
        BlinkStep(true, 500L),
        BlinkStep(false, 0L)
    )

    // No answers yet (empty): 1 short (150) + 1 long (800), 200 ms gap
    val noAnswersYet = listOf(
        BlinkStep(true, 150L),
        BlinkStep(false, 200L),
        BlinkStep(true, 800L),
        BlinkStep(false, 0L)
    )

    // Server unreachable (ESP-side network failure): 3 fast (120/120)
    val serverUnreachable = listOf(
        BlinkStep(true, 120L),
        BlinkStep(false, 120L),
        BlinkStep(true, 120L),
        BlinkStep(false, 120L),
        BlinkStep(true, 120L),
        BlinkStep(false, 0L)
    )

    // Server/AI error: 5 fast (120/120)
    val serverError = listOf(
        BlinkStep(true, 120L),
        BlinkStep(false, 120L),
        BlinkStep(true, 120L),
        BlinkStep(false, 120L),
        BlinkStep(true, 120L),
        BlinkStep(false, 120L),
        BlinkStep(true, 120L),
        BlinkStep(false, 120L),
        BlinkStep(true, 120L),
        BlinkStep(false, 0L)
    )
}
