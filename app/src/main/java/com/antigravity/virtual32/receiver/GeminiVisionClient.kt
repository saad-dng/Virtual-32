package com.antigravity.virtual32.receiver

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Base64
import java.util.concurrent.TimeUnit

sealed class GeminiResult {
    data class Success(val text: String, val rawResponse: String) : GeminiResult()
    data class Error(val message: String, val code: Int? = null) : GeminiResult()
}

/**
 * Client for Google Gemini 1.5 Flash Vision API.
 * Takes raw JPEG bytes from Phone 2 capture and returns real-time scene description for TTS audio out.
 */
class GeminiVisionClient(
    private val client: OkHttpClient = createDefaultClient(),
    private val baseUrl: String = BASE_URL
) {
    companion object {
        private const val TAG = "GeminiVisionClient"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

        fun createDefaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(25, TimeUnit.SECONDS)
                .writeTimeout(25, TimeUnit.SECONDS)
                .build()
        }
    }

    suspend fun analyzeImage(
        jpegBytes: ByteArray,
        apiKey: String,
        prompt: String = "Describe what you see briefly and concisely in one or two sentences for an earbud audio assistant."
    ): GeminiResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext GeminiResult.Error("Gemini API key is not configured. Enter your key in Settings.")
        }

        try {
            val base64Image = Base64.getEncoder().encodeToString(jpegBytes)

            // Build Gemini generateContent payload
            val rootJson = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    val contentObj = JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            // Text prompt part
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            // Inline image part
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mime_type", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inline_data", inlineData)
                            })
                        }
                        put("parts", partsArray)
                    }
                    put(contentObj)
                }
                put("contents", contentsArray)
            }

            val requestUrl = "$baseUrl?key=${apiKey.trim()}"
            val requestBody = rootJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url(requestUrl)
                .post(requestBody)
                .build()

            Log.d(TAG, "Sending ${jpegBytes.size} bytes frame to Gemini 1.5 Flash...")

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                val code = response.code

                if (!response.isSuccessful) {
                    val errorMessage = runCatching {
                        JSONObject(bodyString).getJSONObject("error").getString("message")
                    }.getOrDefault("HTTP $code: $bodyString")
                    Log.e(TAG, "Gemini API error: $errorMessage")
                    return@withContext GeminiResult.Error(errorMessage, code)
                }

                val responseJson = JSONObject(bodyString)
                val candidates = responseJson.optJSONArray("candidates")
                if (candidates == null || candidates.length() == 0) {
                    return@withContext GeminiResult.Error("No candidates returned from Gemini Vision", code)
                }

                val candidate = candidates.getJSONObject(0)
                val parts = candidate.getJSONObject("content").getJSONArray("parts")
                val textBuilder = StringBuilder()
                for (i in 0 until parts.length()) {
                    textBuilder.append(parts.getJSONObject(i).optString("text"))
                }

                val generatedText = textBuilder.toString().trim()
                Log.d(TAG, "Gemini Vision response: $generatedText")
                GeminiResult.Success(generatedText, bodyString)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception calling Gemini API", e)
            GeminiResult.Error(e.message ?: "Failed communicating with Gemini API")
        }
    }
}
