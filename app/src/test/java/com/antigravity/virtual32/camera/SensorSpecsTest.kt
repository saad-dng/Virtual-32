package com.antigravity.virtual32.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class SensorSpecsTest {

    @Test
    fun testOv3660Resolutions_are4by3AspectRatio() {
        Ov3660Resolution.entries.forEach { res ->
            val ratio = res.width.toFloat() / res.height.toFloat()
            // 4:3 is 1.3333334
            assertEquals(
                "Resolution ${res.name} (${res.width}x${res.height}) must be 4:3",
                4f / 3f,
                ratio,
                0.01f
            )
        }
    }

    @Test
    fun testOv3660DefaultResolution_isSVGA() {
        assertEquals(Ov3660Resolution.SVGA, Ov3660Resolution.DEFAULT)
        assertEquals(800, Ov3660Resolution.DEFAULT.width)
        assertEquals(600, Ov3660Resolution.DEFAULT.height)
    }

    @Test
    fun testFromLabel_resolvesCorrectly() {
        assertEquals(Ov3660Resolution.VGA, Ov3660Resolution.fromLabel("VGA (640x480)"))
        assertEquals(Ov3660Resolution.UXGA, Ov3660Resolution.fromLabel("UXGA"))
        assertEquals(Ov3660Resolution.DEFAULT, Ov3660Resolution.fromLabel("UNKNOWN_RESOLUTION"))
    }

    @Test
    fun testJpegQualityPresets() {
        assertEquals(65, JpegQualityPreset.FAST.qualityPercentage)
        assertEquals(80, JpegQualityPreset.BALANCED.qualityPercentage)
        assertEquals(92, JpegQualityPreset.FINE.qualityPercentage)
        assertEquals(JpegQualityPreset.BALANCED, JpegQualityPreset.DEFAULT)

        assertEquals(JpegQualityPreset.FAST, JpegQualityPreset.fromQuality(60))
        assertEquals(JpegQualityPreset.BALANCED, JpegQualityPreset.fromQuality(82))
        assertEquals(JpegQualityPreset.FINE, JpegQualityPreset.fromQuality(95))
    }
}
