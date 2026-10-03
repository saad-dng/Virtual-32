package com.antigravity.virtual32.receiver.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

class ClaudeProvider(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String = "claude-3-5-sonnet-20241022",
    private val baseUrl: String = "https://api.anthropic.com",
    private val downscaleEnabled: Boolean = true
) : VisionProvider {

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Pair(false, "API Key is empty")
        }
        val rawModel = model.trim().ifBlank { "claude-3-5-sonnet-20241022" }
        val cleanModel = if (rawModel == "claude-sonnet-5-5") "claude-3-5-sonnet-20241022" else rawModel
        try {
            val jsonPayload = buildJsonObject {
                put("model", cleanModel)
                put("max_tokens", 5)
                put("messages", buildJsonArray {
                    add(buildJsonObject {
                        put("role", "user")
                        put("content", "ping")
                    })
                })
            }

            val request = Request.Builder()
                .url(baseUrl.trimEnd('/') + "/v1/messages")
                .header("x-api-key", cleanKey)
                .header("anthropic-version", "2023-06-01")
                .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (response.isSuccessful) {
                    Pair(true, "Key & model ($cleanModel) verified successfully!")
                } else {
                    val msg = extractErrorMessage(body)
                    Pair(false, "Claude error (${response.code}): $msg")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    override suspend fun analyze(jpegs: List<ByteArray>, instruction: String): RawAiResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val rawModel = model.trim().ifBlank { "claude-3-5-sonnet-20241022" }
        val cleanModel = if (rawModel == "claude-sonnet-5-5") "claude-3-5-sonnet-20241022" else rawModel

        val totalBytes = jpegs.sumOf { it.size.toLong() }
        val processedJpegs = if (downscaleEnabled && totalBytes > 15 * 1024 * 1024) {
            jpegs.map { com.antigravity.virtual32.util.ImageResizer.downscaleForSession(it, maxSide = 1600, quality = 85) }
        } else {
            jpegs
        }

        val n = processedJpegs.size
        val jsonPayload = buildJsonObject {
            put("model", cleanModel)
            put("max_tokens", 1000)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        processedJpegs.forEachIndexed { index, jpeg ->
                            val i = index + 1
                            add(buildJsonObject {
                                put("type", "text")
                                put("text", "Photo $i of $n:")
                            })
                            val base64Image = Base64.getEncoder().encodeToString(jpeg)
                            add(buildJsonObject {
                                put("type", "image")
                                put("source", buildJsonObject {
                                    put("type", "base64")
                                    put("media_type", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", instruction)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url(baseUrl.trimEnd('/') + "/v1/messages")
            .header("x-api-key", cleanKey)
            .header("anthropic-version", "2023-06-01")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                val err = extractErrorMessage(body)
                throw ProviderException(response.code, retryAfter, "Claude error: ${response.code} $err")
            }

            val json = Json.parseToJsonElement(body).jsonObject
            val content = json["content"]?.jsonArray
            val text = content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

            return@use AiResponseParser.parse(text)
        }
    }

    companion object {
        fun extractErrorMessage(body: String): String {
            return try {
                val json = Json.parseToJsonElement(body).jsonObject
                json["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content ?: body
            } catch (e: Exception) {
                body.take(150)
            }
        }
    }
}
