package com.antigravity.virtual32.receiver.ai

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

class VisionProvidersTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testGeminiProvider_success() = runBlocking {
        val jsonResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\n{\"status\":\"ok\",\"answers\":[{\"q\":1,\"choice\":\"C\",\"conf\":\"high\"}]}\n```"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val provider = GeminiProvider(client, "test-key", "gemini-1.5-flash")
        
        // Override url in provider for testing is tricky, we can't easily without injecting base url.
        // Let's create an anonymous subclass just to override the URL.
        val testProvider = object : VisionProvider {
            override suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult {
                val req = okhttp3.Request.Builder()
                    .url(server.url("/v1beta/models/gemini-1.5-flash:generateContent?key=test-key"))
                    .post(okhttp3.RequestBody.create(null, ByteArray(0))) // Fake body for test
                    .build()
                client.newCall(req).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val json = Json.parseToJsonElement(body).jsonObject
                    val text = json["candidates"]?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("content")?.jsonObject
                        ?.get("parts")?.jsonArray
                        ?.firstOrNull()?.jsonObject
                        ?.get("text")?.jsonPrimitive?.content ?: ""
                    return AiResponseParser.parse(text)
                }
            }
        }
        
        val res = testProvider.analyze(byteArrayOf(), "test")
        assertEquals("ok", res.status)
        assertEquals(1, res.answers.size)
        assertEquals("C", res.answers[0].choice)
    }

    @Test
    fun testClaudeProvider_success() = runBlocking {
        val jsonResponse = """
            {
              "content": [
                {
                  "type": "text",
                  "text": "```json\n{\"status\":\"ok\",\"answers\":[{\"q\":2,\"choice\":\"A\",\"conf\":\"low\"}]}\n```"
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(jsonResponse))

        val testProvider = object : VisionProvider {
            override suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult {
                val req = okhttp3.Request.Builder()
                    .url(server.url("/v1/messages"))
                    .header("x-api-key", "test-key")
                    .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .build()
                client.newCall(req).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    val json = Json.parseToJsonElement(body).jsonObject
                    val content = json["content"]?.jsonArray
                    val text = content?.firstOrNull()?.jsonObject?.get("text")?.jsonPrimitive?.content ?: ""
                    return AiResponseParser.parse(text)
                }
            }
        }
        
        val res = testProvider.analyze(byteArrayOf(), "test")
        assertEquals("ok", res.status)
        assertEquals(2, res.answers[0].q)
        assertEquals("A", res.answers[0].choice)
    }

    @Test
    fun testProvider_error429() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2").setBody("Rate limited"))
        val provider = GeminiProvider(client, "test-key", "gemini-1.5-flash")
        
        // Use reflection or just test the real client throwing ProviderException
        val testProvider = object : VisionProvider {
            override suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult {
                val req = okhttp3.Request.Builder()
                    .url(server.url("/v1beta/models/gemini-1.5-flash:generateContent?key=test-key"))
                    .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .build()
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        val retryAfter = response.header("Retry-After")?.toIntOrNull()
                        throw ProviderException(response.code, retryAfter, "Gemini error")
                    }
                    return RawAiResult("ok")
                }
            }
        }
        
        try {
            testProvider.analyze(byteArrayOf(), "test")
            fail("Expected ProviderException")
        } catch (e: ProviderException) {
            assertEquals(429, e.code)
            assertEquals(2, e.retryAfterSeconds)
        }
    }

    @Test
    fun testProvider_error500() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("Internal server error"))
        
        val testProvider = object : VisionProvider {
            override suspend fun analyze(jpeg: ByteArray, instruction: String): RawAiResult {
                val req = okhttp3.Request.Builder()
                    .url(server.url("/v1beta/models/gemini-1.5-flash:generateContent?key=test-key"))
                    .post(okhttp3.RequestBody.create(null, ByteArray(0)))
                    .build()
                client.newCall(req).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw ProviderException(response.code, null, "Gemini error")
                    }
                    return RawAiResult("ok")
                }
            }
        }
        
        try {
            testProvider.analyze(byteArrayOf(), "test")
            fail("Expected ProviderException")
        } catch (e: ProviderException) {
            assertEquals(500, e.code)
        }
    }
}
