package com.antigravity.virtual32.receiver.ai

import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

class ClaudeProvider(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String = "claude-3-5-sonnet-20240620"
) : VisionProvider {

    override suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult {
        val base64Image = Base64.getEncoder().encodeToString(jpeg)
        
        val jsonPayload = buildJsonObject {
            put("model", model)
            put("max_tokens", 1000)
            put("messages", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("content", buildJsonArray {
                        add(buildJsonObject {
                            put("type", "image")
                            put("source", buildJsonObject {
                                put("type", "base64")
                                put("media_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                        add(buildJsonObject {
                            put("type", "text")
                            put("text", instruction)
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://api.anthropic.com/v1/messages")
            .header("x-api-key", apiKey)
            .header("anthropic-version", "2023-06-01")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                throw ProviderException(response.code, retryAfter, "Claude error: ${response.code} $body")
            }

            val json = Json.parseToJsonElement(body).jsonObject
            val content = json["content"]?.jsonArray
            val text = content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""

            return AiResponseParser.parse(text)
        }
    }
}
