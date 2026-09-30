package com.antigravity.virtual32.receiver.ai

import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Base64

class GeminiProvider(
    private val client: OkHttpClient,
    private val apiKey: String,
    private val model: String = "gemini-1.5-flash"
) : VisionProvider {

    override suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult {
        val base64Image = Base64.getEncoder().encodeToString(jpeg)
        
        val jsonPayload = buildJsonObject {
            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", instruction) })
                        add(buildJsonObject {
                            put("inlineData", buildJsonObject {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
        }

        val request = Request.Builder()
            .url("https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=$apiKey")
            .post(jsonPayload.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val retryAfter = response.header("Retry-After")?.toIntOrNull()
                throw ProviderException(response.code, retryAfter, "Gemini error: ${response.code} $body")
            }

            val json = Json.parseToJsonElement(body).jsonObject
            val candidates = json["candidates"]?.jsonArray
            val text = candidates?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.firstOrNull()?.jsonObject
                ?.get("text")?.jsonPrimitive?.content ?: ""

            return AiResponseParser.parse(text)
        }
    }
}
