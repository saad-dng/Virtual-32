package com.antigravity.virtual32.settings

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import androidx.datastore.preferences.core.emptyPreferences
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class SettingsSmokeTest {
    @Test
    fun testDefaultSettings() = runBlocking {
        // We can test the default mapping logic directly if we just mock the Datastore or construct AppSettings
        val defaultSettings = AppSettings()
        
        assertEquals(5000, defaultSettings.serverPort)
        assertEquals(AiProvider.GEMINI, defaultSettings.provider)
        assertEquals(AiProvider.NONE, defaultSettings.fallbackProvider)
        assertEquals("gemini-1.5-flash", defaultSettings.geminiModel)
        assertEquals("claude-sonnet-5-5", defaultSettings.claudeModel)
        assertEquals("", defaultSettings.geminiKey)
        assertEquals("", defaultSettings.claudeKey)
        assertEquals("default", defaultSettings.activePromptId)
        assertEquals(AnswerMode.REPLACE, defaultSettings.answerMode)
        assertEquals(true, defaultSettings.saveToGallery)
        assertEquals(false, defaultSettings.pauseAi)
        assertEquals(40, defaultSettings.requestTimeoutSec)
        assertEquals("127.0.0.1", defaultSettings.simHost)
        assertEquals(5000, defaultSettings.simPort)
        assertEquals(false, defaultSettings.simLoopback)
        assertEquals("UXGA", defaultSettings.simResolution)
        assertEquals(80, defaultSettings.simJpegQuality)
    }
}
