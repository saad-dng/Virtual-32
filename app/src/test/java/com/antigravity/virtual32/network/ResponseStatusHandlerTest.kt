package com.antigravity.virtual32.network

import androidx.compose.ui.graphics.Color
import com.antigravity.virtual32.ui.theme.LedIdle
import com.antigravity.virtual32.ui.theme.LedRedError
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
class ResponseStatusHandlerTest {

    private val handler = ResponseStatusHandler()

    @Test
    fun testHttp200Success_staysGray_noVibration() {
        val result = NetworkResult.Success(200, "{\"status\":\"ok\"}")
        val feedback = handler.processResult(result)

        assertEquals(LedIdle, feedback.ledColor)
        assertEquals("HTTP 200 OK (GRAY)", feedback.statusLabel)
        assertTrue(feedback.vibrationPattern.isEmpty())
    }

    @Test
    fun testHttp422ValidationError_turnsRed_oneSecondBuzz() {
        val result = NetworkResult.ClientError422(422, "Invalid image format")
        val feedback = handler.processResult(result)

        assertEquals(LedRedError, feedback.ledColor)
        assertEquals("HTTP 422 (RED)", feedback.statusLabel)
        assertArrayEquals(longArrayOf(0, 1000), feedback.vibrationPattern)
    }

    @Test
    fun testTimeout_turnsAmber_twoShortPulses() {
        val result = NetworkResult.Timeout("Deadline exceeded")
        val feedback = handler.processResult(result)

        assertEquals(Color(0xFFFFB300), feedback.ledColor)
        assertEquals("TIMEOUT (AMBER)", feedback.statusLabel)
        assertArrayEquals(longArrayOf(0, 200, 100, 200), feedback.vibrationPattern)
    }

    @Test
    fun testNoNetwork_turnsBlue_threeRapidPulses() {
        val result = NetworkResult.NoNetwork("Hotspot disconnected")
        val feedback = handler.processResult(result)

        assertEquals(Color(0xFF1E88E5), feedback.ledColor)
        assertEquals("NO NETWORK (BLUE)", feedback.statusLabel)
        assertArrayEquals(longArrayOf(0, 100, 100, 100, 100, 100), feedback.vibrationPattern)
    }

    @Test
    fun testHttp5xxServerError_turnsPurple_longShortBuzz() {
        val result = NetworkResult.ServerError5xx(500, "Internal Server Error")
        val feedback = handler.processResult(result)

        assertEquals(Color(0xFF8E24AA), feedback.ledColor)
        assertEquals("HTTP 500 (PURPLE)", feedback.statusLabel)
        assertArrayEquals(longArrayOf(0, 500, 150, 200), feedback.vibrationPattern)
    }

    @Test
    fun testMalformedResponse_turnsOrange_mediumPulse() {
        val result = NetworkResult.MalformedResponse(418, "Unexpected status")
        val feedback = handler.processResult(result)

        assertEquals(Color(0xFFFB8C00), feedback.ledColor)
        assertEquals("MALFORMED (ORANGE)", feedback.statusLabel)
        assertArrayEquals(longArrayOf(0, 400), feedback.vibrationPattern)
    }
}
