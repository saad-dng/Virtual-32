package com.antigravity.virtual32.receiver

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GeminiVisionClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: GeminiVisionClient
    private val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val mockBaseUrl = server.url("/generateContent").toString()
        client = GeminiVisionClient(baseUrl = mockBaseUrl)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testAnalyzeImage_blankKey_returnsError() = runBlocking {
        val result = client.analyzeImage(dummyJpeg, apiKey = "")
        assertTrue(result is GeminiResult.Error)
        assertEquals("Gemini API key is not configured. Enter your key in Settings.", (result as GeminiResult.Error).message)
    }

    @Test
    fun testAnalyzeImage_successResponse_parsesText() = runBlocking {
        val mockGeminiJson = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "There is a blue notebook and a coffee cup on the desk."
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody(mockGeminiJson)
        )

        val result = client.analyzeImage(dummyJpeg, apiKey = "test_key", prompt = "What do you see?")
        assertTrue(result is GeminiResult.Success)
        val success = result as GeminiResult.Success
        assertEquals("There is a blue notebook and a coffee cup on the desk.", success.text)

        val recordedRequest = server.takeRequest()
        assertTrue(recordedRequest.requestUrl?.queryParameter("key") == "test_key")
        val body = recordedRequest.body.readUtf8()
        assertTrue(body.contains("What do you see?"))
        assertTrue(body.contains("image/jpeg"))
    }

    @Test
    fun testAnalyzeImage_apiError_returnsErrorResult() = runBlocking {
        val errorJson = """
            {
              "error": {
                "code": 400,
                "message": "API key not valid. Please pass a valid API key."
              }
            }
        """.trimIndent()

        server.enqueue(
            MockResponse()
                .setResponseCode(400)
                .setBody(errorJson)
        )

        val result = client.analyzeImage(dummyJpeg, apiKey = "invalid_key")
        assertTrue(result is GeminiResult.Error)
        val error = result as GeminiResult.Error
        assertEquals("API key not valid. Please pass a valid API key.", error.message)
    }
}
