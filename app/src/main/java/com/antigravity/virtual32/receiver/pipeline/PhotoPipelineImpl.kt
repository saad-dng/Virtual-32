package com.antigravity.virtual32.receiver.pipeline

import android.util.Log
import com.antigravity.virtual32.data.AnswerStore
import com.antigravity.virtual32.data.Batch
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.receiver.ai.ClaudeProvider
import com.antigravity.virtual32.receiver.ai.GeminiProvider
import com.antigravity.virtual32.receiver.ai.PromptBuilder
import com.antigravity.virtual32.receiver.ai.ProviderException
import com.antigravity.virtual32.receiver.ai.RawAiResult
import com.antigravity.virtual32.receiver.ai.VisionProvider
import com.antigravity.virtual32.settings.AiProvider
import com.antigravity.virtual32.settings.AnswerMode
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.settings.PromptRepository
import com.antigravity.virtual32.util.GalleryWriter
import com.antigravity.virtual32.util.ImageResizer
import com.antigravity.virtual32.util.PhotoCache
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import android.content.Context
import android.os.Vibrator
import android.os.VibrationEffect
import android.os.Build

data class PipelineState(
    val queueLength: Int = 0,
    val isAnalyzing: Boolean = false,
    val currentStatus: String? = null,
    val lastLatencyMs: Long = 0L,
    val isPaused: Boolean = false,
    val lastPhotoPath: String? = null
)

data class PhotoSet(
    val jpegs: List<ByteArray>,
    val source: String = "ESP",
    val cachedPaths: List<String> = emptyList(),
    val galleryUris: List<String> = emptyList()
)

class PhotoPipelineImpl(
    private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val promptRepo: PromptRepository,
    private val answerStore: RoomAnswerStore,
    private val okHttpClient: OkHttpClient,
    private val galleryWriter: GalleryWriter,
    private val photoCache: PhotoCache,
    private val isNetworkAvailable: () -> Boolean
) : PhotoPipeline {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _state = MutableStateFlow(PipelineState())
    val state: StateFlow<PipelineState> = _state.asStateFlow()

    private val queue = Channel<QueueItem>(capacity = 20)
    
    data class QueueItem(
        val photoSet: PhotoSet,
        val resultChannel: Channel<String>
    )

    init {
        scope.launch {
            workerLoop()
        }
    }

    override suspend fun processPhoto(jpeg: ByteArray, source: String): String {
        return processPhotos(listOf(jpeg), source)
    }

    override suspend fun processPhotos(
        jpegs: List<ByteArray>,
        source: String,
        cachedPaths: List<String>,
        galleryUris: List<String>
    ): String {
        val settings = settingsRepo.getSettings()
        if (settings.pauseAi) {
            return buildJsonObject {
                put("status", "error")
                put("reason", "paused")
                put("pages", jpegs.size)
                put("warnings", buildJsonArray {})
            }.toString()
        }

        val resultChannel = Channel<String>(1)
        val item = QueueItem(PhotoSet(jpegs, source, cachedPaths, galleryUris), resultChannel)
        
        val added = queue.trySend(item).isSuccess
        if (!added) {
            Log.w("PhotoPipeline", "Queue full, dropping item")
            return buildJsonObject {
                put("status", "error")
                put("reason", "queue_full")
                put("pages", jpegs.size)
                put("warnings", buildJsonArray {})
            }.toString()
        }
        
        updateState { it.copy(queueLength = it.queueLength + 1) }
        return resultChannel.receive()
    }

    private suspend fun workerLoop() {
        while (scope.isActive) {
            val item = queue.receive()
            val n = item.photoSet.jpegs.size
            updateState { it.copy(queueLength = it.queueLength - 1, isAnalyzing = true, currentStatus = "Analyzing") }
            val startTime = System.currentTimeMillis()
            
            val settings = settingsRepo.getSettings()
            val baseTimeoutMs = settings.requestTimeoutSec * 1000L
            val itemTimeoutMs = minOf(150_000L, baseTimeoutMs + 8_000L * n)

            val resultJson = try {
                kotlinx.coroutines.withTimeout(itemTimeoutMs) {
                    processItemWithRetries(item.photoSet)
                }
            } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
                Log.w("PhotoPipeline", "Worker timed out after ${itemTimeoutMs}ms")
                buildJsonObject {
                    put("status", "error")
                    put("reason", "timeout")
                    put("pages", n)
                    put("warnings", buildJsonArray {})
                }.toString()
            } catch (e: Exception) {
                Log.e("PhotoPipeline", "Worker failed", e)
                buildJsonObject {
                    put("status", "error")
                    put("reason", "ai_failed")
                    put("pages", n)
                    put("warnings", buildJsonArray {})
                }.toString()
            }
            
            val latency = System.currentTimeMillis() - startTime
            val status = if (resultJson.contains("Model retired")) {
                "Model retired - change it in Settings > AI & Prompt"
            } else {
                "Done"
            }
            updateState { it.copy(isAnalyzing = false, currentStatus = status, lastLatencyMs = latency) }
            
            item.resultChannel.send(resultJson)
        }
    }

    private suspend fun processItemWithRetries(photoSet: PhotoSet): String {
        val startTime = System.currentTimeMillis()
        while (!isNetworkAvailable()) {
            updateState { it.copy(currentStatus = "Waiting for network", isPaused = true) }
            delay(1000)
        }
        updateState { it.copy(isPaused = false, currentStatus = "Analyzing") }

        val settings = settingsRepo.getSettings()
        val n = photoSet.jpegs.size
        
        val finalCachedPaths = mutableListOf<String>()
        val finalGalleryUris = mutableListOf<String>()

        if (photoSet.cachedPaths.isNotEmpty() && photoSet.cachedPaths.size == photoSet.jpegs.size) {
            finalCachedPaths.addAll(photoSet.cachedPaths)
            finalGalleryUris.addAll(photoSet.galleryUris)
        } else {
            val batchIdStr = java.util.UUID.randomUUID().toString().substring(0, 8)
            photoSet.jpegs.forEachIndexed { idx, jpeg ->
                val pId = if (photoSet.jpegs.size > 1) "${batchIdStr}_p${idx + 1}" else batchIdStr
                if (settings.saveToGallery) {
                    val gUri = galleryWriter.savePhoto(jpeg, photoSet.source, pId)
                    if (gUri != null) finalGalleryUris.add(gUri)
                }
                val cPath = photoCache.saveInternal(jpeg, "b${pId}.jpg")
                if (cPath != null) finalCachedPaths.add(cPath)
            }
        }

        updateState { it.copy(lastPhotoPath = finalCachedPaths.firstOrNull()) }

        val primaryProvider = if (settings.provider == AiProvider.GEMINI) {
            GeminiProvider(okHttpClient, settings.geminiKey, settings.geminiModel, downscaleEnabled = settings.downscaleSessionPayload)
        } else {
            ClaudeProvider(okHttpClient, settings.claudeKey, settings.claudeModel, downscaleEnabled = settings.downscaleSessionPayload)
        }
        
        val fallbackProvider = when (settings.fallbackProvider) {
            AiProvider.GEMINI -> if (settings.geminiKey.isNotBlank()) GeminiProvider(okHttpClient, settings.geminiKey, settings.geminiModel, downscaleEnabled = settings.downscaleSessionPayload) else null
            AiProvider.CLAUDE -> if (settings.claudeKey.isNotBlank()) ClaudeProvider(okHttpClient, settings.claudeKey, settings.claudeModel, downscaleEnabled = settings.downscaleSessionPayload) else null
            AiProvider.NONE -> null
        }

        val prompts = promptRepo.presetsFlow.first()
        val preset = prompts.find { it.id == settings.activePromptId } ?: promptRepo.defaultPresets.first()
        var instruction = PromptBuilder.build(
            preset.instruction,
            includeReasoning = settings.includeReasoning,
            photoCount = n,
            multiPhotoInstruction = settings.multiPhotoInstruction
        )

        // Attempt 1
        var res = attemptAnalyze(primaryProvider, photoSet.jpegs, instruction)
        if (res.isParseError) {
            instruction = PromptBuilder.build(
                preset.instruction,
                includeReasoning = settings.includeReasoning,
                isRetry = true,
                photoCount = n,
                multiPhotoInstruction = settings.multiPhotoInstruction
            )
            res = attemptAnalyze(primaryProvider, photoSet.jpegs, instruction)
        }

        // Retries for network/5xx/429
        if (isRetryableError(res)) {
            delay(1000)
            res = attemptAnalyze(primaryProvider, photoSet.jpegs, instruction)
            if (isRetryableError(res)) {
                delay(3000)
                res = attemptAnalyze(primaryProvider, photoSet.jpegs, instruction)
            }
        }

        // Fallback
        if (res.status == "error" && fallbackProvider != null) {
            Log.i("PhotoPipeline", "Primary failed, using fallback")
            res = attemptAnalyze(fallbackProvider, photoSet.jpegs, instruction)
        }

        val batch = Batch(
            source = photoSet.source,
            photoPath = finalCachedPaths.firstOrNull(),
            galleryUri = finalGalleryUris.firstOrNull(),
            status = res.status,
            provider = settings.provider.name,
            model = if (settings.provider == AiProvider.GEMINI) settings.geminiModel else settings.claudeModel,
            promptName = settings.activePromptId,
            promptHash = instruction.hashCode().toString(),
            latencyMs = System.currentTimeMillis() - startTime,
            rawResponse = res.reason,
            pageCount = n,
            photoPaths = Json.encodeToString(finalCachedPaths),
            galleryUris = Json.encodeToString(finalGalleryUris),
            warnings = Json.encodeToString(res.warnings)
        )

        val batchId: Long
        if (res.status == "ok") {
            val mode = if (settings.answerMode == AnswerMode.REPLACE) "REPLACE" else "APPEND"
            batchId = answerStore.applyBatch(batch, res.answers, mode)

            if (settings.enableHaptics) {
                try {
                    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator?.vibrate(100)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } else {
            // Still insert the batch to track history
            batchId = answerStore.applyBatch(batch, emptyList(), "APPEND")
        }

        return encodeResult(res, n, batchId)
    }

    private suspend fun attemptAnalyze(provider: VisionProvider, jpegs: List<ByteArray>, instruction: String): RawAiResult {
        return try {
            provider.analyze(jpegs, instruction)
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
        if (res.status != "error") return false
        val reason = res.reason.orEmpty()
        if (reason.contains("retired", ignoreCase = true) || reason.contains("not found", ignoreCase = true)) return false
        return reason.contains("timeout") || reason.contains("50") || reason.contains("429")
    }

    private fun updateState(updater: (PipelineState) -> PipelineState) {
        _state.value = updater(_state.value)
    }

    private fun encodeResult(res: RawAiResult, pages: Int, batchId: Long): String {
        return buildJsonObject {
            put("status", res.status)
            if (res.status == "ok") {
                put("count", res.answers.size)
                put("batch", batchId)
            } else {
                if (res.reason != null) put("reason", res.reason)
            }
            put("pages", pages)
            put("warnings", buildJsonArray {
                res.warnings.forEach { add(JsonPrimitive(it)) }
            })
        }.toString()
    }
}
