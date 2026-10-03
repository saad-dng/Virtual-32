package com.antigravity.virtual32.receiver.ai

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class MultiImageProviderRequestShapeTest {
    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testGemini_threeImages_requestShape() = runBlocking {
        val geminiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"status\":\"ok\",\"answers\":[{\"q\":1,\"choice\":\"A\",\"conf\":\"high\",\"page\":1}]}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(geminiResponse))

        val provider = GeminiProvider(
            client = client,
            apiKey = "test-gemini-key",
            model = "gemini-3.8-flash",
            baseUrl = server.url("/").toString()
        )

        val img1 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x11, 0xFF.toByte(), 0xD9.toByte())
        val img2 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x22, 0xFF.toByte(), 0xD9.toByte())
        val img3 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x33, 0xFF.toByte(), 0xD9.toByte())

        val instruction = "Solve every question carefully."
        val result = provider.analyze(listOf(img1, img2, img3), instruction)
        assertEquals("ok", result.status)

        val recordedRequest = server.takeRequest(5, TimeUnit.SECONDS)!!
        val requestBody = recordedRequest.body.readUtf8()
        val json = Json.parseToJsonElement(requestBody).jsonObject

        val parts = json["contents"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("parts")?.jsonArray ?: error("No parts found")

        assertEquals(7, parts.size)

        // Part 0: instruction text
        assertEquals(instruction, parts[0].jsonObject["text"]?.jsonPrimitive?.content)

        // Part 1: Photo 1 of 3:
        assertEquals("Photo 1 of 3:", parts[1].jsonObject["text"]?.jsonPrimitive?.content)
        // Part 2: image 1 inlineData
        assertTrue(parts[2].jsonObject.containsKey("inlineData"))

        // Part 3: Photo 2 of 3:
        assertEquals("Photo 2 of 3:", parts[3].jsonObject["text"]?.jsonPrimitive?.content)
        // Part 4: image 2 inlineData
        assertTrue(parts[4].jsonObject.containsKey("inlineData"))

        // Part 5: Photo 3 of 3:
        assertEquals("Photo 3 of 3:", parts[5].jsonObject["text"]?.jsonPrimitive?.content)
        // Part 6: image 3 inlineData
        assertTrue(parts[6].jsonObject.containsKey("inlineData"))
    }

    @Test
    fun testClaude_threeImages_requestShape() = runBlocking {
        val claudeResponse = """
            {
              "content": [
                {
                  "type": "text",
                  "text": "{\"status\":\"ok\",\"answers\":[{\"q\":1,\"choice\":\"A\",\"conf\":\"high\",\"page\":1}]}"
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(claudeResponse))

        val provider = ClaudeProvider(
            client = client,
            apiKey = "test-claude-key",
            model = "claude-3-5-sonnet-20241022",
            baseUrl = server.url("/").toString()
        )

        val img1 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x11, 0xFF.toByte(), 0xD9.toByte())
        val img2 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x22, 0xFF.toByte(), 0xD9.toByte())
        val img3 = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x33, 0xFF.toByte(), 0xD9.toByte())

        val instruction = "Solve all questions in order."
        val result = provider.analyze(listOf(img1, img2, img3), instruction)
        assertEquals("ok", result.status)

        val recordedRequest = server.takeRequest(5, TimeUnit.SECONDS)!!
        val requestBody = recordedRequest.body.readUtf8()
        val json = Json.parseToJsonElement(requestBody).jsonObject

        val content = json["messages"]?.jsonArray
            ?.firstOrNull()?.jsonObject
            ?.get("content")?.jsonArray ?: error("No content found")

        assertEquals(7, content.size)

        // Block 0: Photo 1 of 3:
        assertEquals("text", content[0].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("Photo 1 of 3:", content[0].jsonObject["text"]?.jsonPrimitive?.content)
        // Block 1: image 1
        assertEquals("image", content[1].jsonObject["type"]?.jsonPrimitive?.content)

        // Block 2: Photo 2 of 3:
        assertEquals("text", content[2].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("Photo 2 of 3:", content[2].jsonObject["text"]?.jsonPrimitive?.content)
        // Block 3: image 2
        assertEquals("image", content[3].jsonObject["type"]?.jsonPrimitive?.content)

        // Block 4: Photo 3 of 3:
        assertEquals("text", content[4].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals("Photo 3 of 3:", content[4].jsonObject["text"]?.jsonPrimitive?.content)
        // Block 5: image 3
        assertEquals("image", content[5].jsonObject["type"]?.jsonPrimitive?.content)

        // Block 6: instruction text
        assertEquals("text", content[6].jsonObject["type"]?.jsonPrimitive?.content)
        assertEquals(instruction, content[6].jsonObject["text"]?.jsonPrimitive?.content)
    }

    @Test
    fun testGemini_downscaleBranch_whenPayloadExceeds15MB() = runBlocking {
        val geminiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"status\":\"ok\",\"answers\":[]}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(geminiResponse))

        val provider = GeminiProvider(
            client = client,
            apiKey = "test-key",
            model = "gemini-3.8-flash",
            baseUrl = server.url("/").toString()
        )

        // 16 MB dummy payload across 2 images
        val bigImg1 = ByteArray(8 * 1024 * 1024)
        bigImg1[0] = 0xFF.toByte()
        bigImg1[1] = 0xD8.toByte()
        val bigImg2 = ByteArray(8 * 1024 * 1024)
        bigImg2[0] = 0xFF.toByte()
        bigImg2[1] = 0xD8.toByte()

        // Calling analyze triggers the >15MB downscale branch
        provider.analyze(listOf(bigImg1, bigImg2), "Prompt")

        val req = server.takeRequest(5, TimeUnit.SECONDS)
        assertTrue(req != null)
    }

    @Test
    fun testClaude_downscaleBranch_whenPayloadExceeds15MB() = runBlocking {
        val claudeResponse = """
            {
              "content": [
                {
                  "type": "text",
                  "text": "{\"status\":\"ok\",\"answers\":[]}"
                }
              ]
            }
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(claudeResponse))

        val provider = ClaudeProvider(
            client = client,
            apiKey = "test-key",
            model = "claude-3-5-sonnet-20241022",
            baseUrl = server.url("/").toString()
        )

        val bigImg1 = ByteArray(8 * 1024 * 1024)
        bigImg1[0] = 0xFF.toByte()
        bigImg1[1] = 0xD8.toByte()
        val bigImg2 = ByteArray(8 * 1024 * 1024)
        bigImg2[0] = 0xFF.toByte()
        bigImg2[1] = 0xD8.toByte()

        provider.analyze(listOf(bigImg1, bigImg2), "Prompt")

        val req = server.takeRequest(5, TimeUnit.SECONDS)
        assertTrue(req != null)
    }
}
