package com.antigravity.virtual32.receiver.pipeline

import android.util.Log
import com.antigravity.virtual32.data.AnswerStore
import com.antigravity.virtual32.receiver.ai.ClaudeProvider
import com.antigravity.virtual32.receiver.ai.GeminiProvider
import com.antigravity.virtual32.receiver.ai.PromptBuilder
import com.antigravity.virtual32.receiver.ai.ProviderException
import com.antigravity.virtual32.receiver.ai.RawAiResult
import com.antigravity.virtual32.receiver.ai.VisionProvider
import com.antigravity.virtual32.settings.AiProvider
import com.antigravity.virtual32.settings.AnswerMode
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.util.ImageResizer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.OkHttpClient

data class PipelineState(
    val queueLength: Int = 0,
    val isAnalyzing: Boolean = false,
    val currentStatus: String? = null,
    val lastLatencyMs: Long = 0L,
    val isPaused: Boolean = false
)

class PhotoPipelineImpl(
    private val settingsRepo: SettingsRepository,
    private val answerStore: AnswerStore,
    private val okHttpClient: OkHttpClient,
    private val isNetworkAvailable: () -> Boolean
) : PhotoPipeline {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow(PipelineState())
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private val queue = Channel<QueueItem>(capacity = 20)
    
    data class QueueItem(
        val jpeg: ByteArray,
        val resultChannel: Channel<String>
    )

    init {
        scope.launch {
            workerLoop()
        }
    }

    override suspend fun processPhoto(jpeg: ByteArray): String {
        val settings = settingsRepo.getSettings()
        if (settings.pauseAi) {
            return buildJsonObject { put("status", "error"); put("reason", "paused") }.toString()
        }

        val resultChannel = Channel<String>(1)
        val item = QueueItem(jpeg, resultChannel)
        
        val added = queue.trySend(item).isSuccess
        if (!added) {
            Log.w("PhotoPipeline", "Queue full, dropping item")
            return buildJsonObject { put("status", "error"); put("reason", "queue_full") }.toString()
        }
        
        updateState { it.copy(queueLength = it.queueLength + 1) }
        
        return resultChannel.receive()
    }

    private suspend fun workerLoop() {
        while (scope.isActive) {
            val item = queue.receive()
            updateState { it.copy(queueLength = it.queueLength - 1, isAnalyzing = true, currentStatus = "Analyzing") }
            val startTime = System.currentTimeMillis()
            
            val resultJson = try {
                processItemWithRetries(item.jpeg)
            } catch (e: Exception) {
                Log.e("PhotoPipeline", "Worker failed", e)
                buildJsonObject { put("status", "error"); put("reason", "ai_failed") }.toString()
            }
            
            val latency = System.currentTimeMillis() - startTime
            updateState { it.copy(isAnalyzing = false, currentStatus = "Done", lastLatencyMs = latency) }
            
            item.resultChannel.send(resultJson)
        }
    }

    private suspend fun processItemWithRetries(jpeg: ByteArray): String {
        while (!isNetworkAvailable()) {
            updateState { it.copy(currentStatus = "Waiting for network", isPaused = true) }
            delay(1000)
        }
        updateState { it.copy(isPaused = false, currentStatus = "Analyzing") }

        val settings = settingsRepo.getSettings()
        val primaryProvider = if (settings.provider == AiProvider.GEMINI) {
            GeminiProvider(okHttpClient, settings.geminiKey)
        } else {
            ClaudeProvider(okHttpClient, settings.claudeKey, settings.claudeModel)
        }
        
        val fallbackProvider = if (settings.provider == AiProvider.GEMINI && settings.claudeKey.isNotBlank()) {
            ClaudeProvider(okHttpClient, settings.claudeKey, settings.claudeModel)
        } else if (settings.provider == AiProvider.CLAUDE && settings.geminiKey.isNotBlank()) {
            GeminiProvider(okHttpClient, settings.geminiKey)
        } else null

        val resizedJpeg = ImageResizer.downscaleIfNeeded(jpeg)
        var instruction = PromptBuilder.build("")

        // Attempt 1
        var res = attemptAnalyze(primaryProvider, resizedJpeg, instruction)
        if (res.isParseError) {
            instruction = PromptBuilder.build("", isRetry = true)
            res = attemptAnalyze(primaryProvider, resizedJpeg, instruction)
        }

        // Retries for network/5xx/429
        if (isRetryableError(res)) {
            delay(1000)
            res = attemptAnalyze(primaryProvider, resizedJpeg, instruction)
            if (isRetryableError(res)) {
                delay(3000)
                res = attemptAnalyze(primaryProvider, resizedJpeg, instruction)
            }
        }

        // Fallback
        if (res.status == "error" && fallbackProvider != null) {
            Log.i("PhotoPipeline", "Primary failed, using fallback")
            res = attemptAnalyze(fallbackProvider, resizedJpeg, instruction)
        }

        if (res.status == "ok") {
            if (settings.answerMode == AnswerMode.REPLACE) {
                answerStore.replace(res.answers)
            } else {
                answerStore.append(res.answers)
            }
        }

        return encodeResult(res)
    }

    private suspend fun attemptAnalyze(provider: VisionProvider, jpeg: ByteArray, instruction: String): RawAiResult {
        return try {
            provider.analyze(jpeg, instruction)
        } catch (e: ProviderException) {
            if (e.retryAfterSeconds != null) {
                delay(e.retryAfterSeconds * 1000L)
            }
            RawAiResult("error", reason = e.message)
        } catch (e: Exception) {
            RawAiResult("error", reason = e.message ?: "unknown_error")
        }
    }

    private fun isRetryableError(res: RawAiResult): Boolean {
        return res.status == "error" && (res.reason?.contains("timeout") == true || res.reason?.contains("50") == true || res.reason?.contains("429") == true)
    }

    private fun updateState(updater: (PipelineState) -> PipelineState) {
        _state.value = updater(_state.value)
    }

    private fun encodeResult(res: RawAiResult): String {
        return buildJsonObject {
            put("status", res.status)
            if (res.status == "ok") {
                put("count", res.answers.size)
                // batch id is not strictly required but could be added
            } else {
                if (res.reason != null) put("reason", res.reason)
            }
        }.toString()
    }
}
