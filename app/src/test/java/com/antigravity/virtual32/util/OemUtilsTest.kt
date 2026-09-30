package com.antigravity.virtual32.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OemUtilsTest {
    @Test
    fun testXiaomiDetection() {
        val guidance = OemUtils.getOemGuidance("xiaomi")
        assertNotNull(guidance)
        assertTrue(guidance!!.contains("Autostart"))
        
        val intents = OemUtils.getOemIntents("Xiaomi")
        assertTrue(intents.isNotEmpty())
        assertEquals("com.miui.securitycenter", intents[0].component?.packageName)
    }

    @Test
    fun testSamsungDetection() {
        val guidance = OemUtils.getOemGuidance("Samsung")
        assertNotNull(guidance)
        assertTrue(guidance!!.contains("Put app to sleep"))

        val intents = OemUtils.getOemIntents("SAMSUNG")
        assertTrue(intents.isNotEmpty())
        assertEquals("com.samsung.android.lool", intents[0].component?.packageName)
    }

    @Test
    fun testUnknownDevice() {
        val guidance = OemUtils.getOemGuidance("Google")
        assertNull(guidance)

        val intents = OemUtils.getOemIntents("Google")
        assertTrue(intents.isEmpty())
    }
}
