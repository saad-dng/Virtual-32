package com.antigravity.virtual32.receiver.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

class GeminiProvider(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String = "gemini-3.8-flash",
    private val baseUrl: String = "https://generativelanguage.googleapis.com",
    private val downscaleEnabled: Boolean = true
) : VisionProvider {

    suspend fun testConnection(): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return@withContext Pair(false, "API Key is empty")
        }
        val cleanModel = model.trim().ifBlank { "gemini-3.8-flash" }.removePrefix("models/")
        try {
            val url = baseUrl.trimEnd('/') + "/v1beta/models/$cleanModel?key=$cleanKey"
            val request = Request.Builder()
                .url(url)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                val isModelRetired = response.code == 404 ||
                    body.contains("not found", ignoreCase = true) ||
                    body.contains("no longer available", ignoreCase = true) ||
                    body.contains("retired", ignoreCase = true)

                if (response.isSuccessful) {
                    Pair(true, "Key & model ($cleanModel) verified successfully!")
                } else if (isModelRetired) {
                    Pair(false, "Model retired - change it in Settings > AI & Prompt")
                } else {
                    val msg = extractErrorMessage(body)
                    Pair(false, "Gemini error (${response.code}): $msg")
                }
            }
        } catch (e: Exception) {
            Pair(false, "Connection failed: ${e.message ?: e.javaClass.simpleName}")
        }
    }

    override suspend fun analyze(jpegs: List<ByteArray>, instruction: String): RawAiResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        val cleanModel = model.trim().ifBlank { "gemini-3.8-flash" }.removePrefix("models/")
        
        val totalBytes = jpegs.sumOf { it.size.toLong() }
        val processedJpegs = if (downscaleEnabled && totalBytes > 15 * 1024 * 1024) {
            jpegs.map { com.antigravity.virtual32.util.ImageResizer.downscaleForSession(it, maxSide = 1600, quality = 85) }
        } else {
            jpegs
        }

        val n = processedJpegs.size
        val jsonPayload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", instruction) })
                        processedJpegs.forEachIndexed { index, jpeg ->
                            val i = index + 1
                            add(buildJsonObject { put("text", "Photo $i of $n:") })
                            val base64Image = Base64.getEncoder().encodeToString(jpeg)
                            add(buildJsonObject {
                                put("inlineData", buildJsonObject {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                })
                            })
                        }
                    })
                })
            })
        }

        val url = baseUrl.trimEnd('/') + "/v1beta/models/$cleanModel:generateContent?key=$cleanKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                val err = extractErrorMessage(body)
                val isModelRetired = response.code == 404 ||
                    err.contains("not found", ignoreCase = true) ||
                    err.contains("no longer available", ignoreCase = true) ||
                    err.contains("retired", ignoreCase = true) ||
                    body.contains("not found", ignoreCase = true) ||
                    body.contains("no longer available", ignoreCase = true)

                if (isModelRetired) {
                    throw ProviderException(404, retryAfter, "Model retired - change it in Settings > AI & Prompt")
                }
                throw ProviderException(response.code, retryAfter, "Gemini error: ${response.code} $err")
            }

            val json = Json.parseToJsonElement(body).jsonObject
            val candidates = json["candidates"]?.jsonArray
            val text = candidates?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: ""

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
