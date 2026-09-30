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
                    val json = kotlinx.serialization.json.Json.parseToJsonElement(body).kotlinx.serialization.json.jsonObject
                    val text = json["candidates"]?.kotlinx.serialization.json.jsonArray
                        ?.firstOrNull()?.kotlinx.serialization.json.jsonObject
                        ?.get("content")?.kotlinx.serialization.json.jsonObject
                        ?.get("parts")?.kotlinx.serialization.json.jsonArray
                        ?.firstOrNull()?.kotlinx.serialization.json.jsonObject
                        ?.get("text")?.kotlinx.serialization.json.jsonPrimitive?.content ?: ""
                    return AiResponseParser.parse(text)
                }
            }
        }
        
        val res = testProvider.analyze(byteArrayOf(), "test")
        assertEquals("ok", res.status)
        assertEquals(1, res.answers.size)
        assertEquals("C", res.answers[0].choice)
    }
}
