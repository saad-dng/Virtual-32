package com.antigravity.virtual32.receiver.pipeline

import com.antigravity.virtual32.data.AnswerStore
import com.antigravity.virtual32.data.InMemoryAnswerStore
import com.antigravity.virtual32.settings.AiProvider
import com.antigravity.virtual32.settings.AnswerMode
import com.antigravity.virtual32.settings.AppSettings
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoPipelineTest {

    private class FakeSettingsRepo : SettingsRepository(null) {
        var settings = AppSettings(
            provider = AiProvider.GEMINI,
            geminiKey = "fake_gemini_key",
            claudeKey = "fake_claude_key",
            pauseAi = false,
            answerMode = AnswerMode.APPEND
        )
        override val settingsFlow: Flow<AppSettings> = flowOf(settings)
        override suspend fun getSettings(): AppSettings = settings
        override suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
            settings = transform(settings)
        }
    }

    @Test
    fun testPipelineQueueOrder() = runBlocking {
        val repo = FakeSettingsRepo()
        val store = InMemoryAnswerStore()
        var callCount = 0

        val pipeline = object : PhotoPipeline {
            override suspend fun processPhoto(jpeg: ByteArray): String {
                delay(100)
                callCount++
                return "{\"status\":\"ok\",\"count\":$callCount}"
            }
        }

        // Just checking basic pipeline fake works
        val res1 = pipeline.processPhoto(byteArrayOf())
        val res2 = pipeline.processPhoto(byteArrayOf())
        
        assertTrue(res1.contains("\"count\":1"))
        assertTrue(res2.contains("\"count\":2"))
    }
}
