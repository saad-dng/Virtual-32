package com.antigravity.virtual32.receiver.server

import com.antigravity.virtual32.data.AnswerStore
import com.antigravity.virtual32.data.NextResult
import com.antigravity.virtual32.receiver.ai.RawAnswer
import com.antigravity.virtual32.receiver.pipeline.PhotoPipeline
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.*
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

class ReceiverHttpServerTest {
    private lateinit var server: ReceiverHttpServer
    private class FakeAnswerStore : AnswerStore {
        var cursorValue = 0
        var countValue = 0
        var nextResult = NextResult(ok = false)
        
        override val cursor: Int get() = cursorValue
        override val count: Int get() = countValue
        override fun next(): NextResult = nextResult
        override fun repeat(): NextResult = nextResult
        override fun reset(): NextResult = nextResult
        override fun replace(newAnswers: List<RawAnswer>) {}
        override fun append(newAnswers: List<RawAnswer>) {}
    }

    private lateinit var answerStore: FakeAnswerStore
    private lateinit var logBuffer: LogBuffer
    private val testPort = 5998

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        answerStore = FakeAnswerStore()
        logBuffer = LogBuffer()
        
        val fakePipeline = object : PhotoPipeline {
            override suspend fun processPhoto(jpeg: ByteArray): String {
                delay(50)
                return "{\"status\":\"ok\",\"count\":3}"
            }
        }
        
        server = ReceiverHttpServer(testPort, fakePipeline, answerStore, logBuffer)
        server.start()
        Thread.sleep(150) // Wait for bind
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testServer_pingEndpoint_returns200() {
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/ping")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            assertEquals("close", response.header("Connection"))
            val body = response.body?.string().orEmpty()
            val json = Json.parseToJsonElement(body).jsonObject
            assertEquals(true, json["ok"]?.jsonPrimitive?.content?.toBooleanStrictOrNull())
            assertEquals("virtual32", json["app"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun testServer_multipartUpload_returns200() {
        val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x12, 0x34, 0xFF.toByte(), 0xD9.toByte())

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "image",
                filename = "frame.jpg",
                body = dummyJpeg.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/upload")
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = response.body?.string().orEmpty()
            assertTrue(body.contains("\"status\":\"ok\""))
        }
    }

    @Test
    fun testServer_rawUpload_returns200() {
        val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x12, 0x34, 0xFF.toByte(), 0xD9.toByte())
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/upload")
            .post(dummyJpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = response.body?.string().orEmpty()
            assertTrue(body.contains("\"status\":\"ok\""))
        }
    }

    @Test
    fun testServer_badImage_returns400() {
        val dummyText = byteArrayOf(0x12, 0x34, 0x56)
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/upload")
            .post(dummyText.toRequestBody("image/jpeg".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(400, response.code)
            val body = response.body?.string().orEmpty()
            assertTrue(body.contains("bad_image"))
        }
    }

    @Test
    fun testServer_oversizedBody_returns413() {
        // Create an 8.1MB string
        val dummyText = ByteArray(8 * 1024 * 1024 + 100)
        
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/upload")
            .post(dummyText.toRequestBody("application/octet-stream".toMediaType()))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                assertEquals(413, response.code)
            }
        } catch (e: java.net.SocketException) {
            // It's acceptable for the server to abruptly close the connection
            assertTrue(true)
        }
    }

    @Test
    fun testServer_answerSequences() {
        // This test assumed answerStore had a setAnswers method.
        // We'll simulate responses via nextResult.
        answerStore.nextResult = NextResult(ok = true, q = 1, choice = "A", blinks = 1)

        fun getReq(path: String): JsonObject {
            val req = Request.Builder().url("http://127.0.0.1:$testPort$path").get().build()
            client.newCall(req).execute().use { response ->
                assertEquals(200, response.code)
                return Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            }
        }

        // next -> 1
        var res = getReq("/next")
        assertEquals(true, res["ok"]?.jsonPrimitive?.content?.toBooleanStrictOrNull())
        assertEquals("1", res["q"]?.jsonPrimitive?.content)

        // repeat -> 1
        answerStore.nextResult = NextResult(ok = true, q = 1, choice = "A", blinks = 1)
        res = getReq("/repeat")
        assertEquals("1", res["q"]?.jsonPrimitive?.content)

        // next -> end
        answerStore.nextResult = NextResult(ok = false, reason = "wrap", end = true)
        res = getReq("/next")
        assertEquals(true, res["end"]?.jsonPrimitive?.content?.toBooleanStrictOrNull())

        // reset
        answerStore.nextResult = NextResult(ok = true, reason = "reset")
        res = getReq("/reset")
        assertEquals(true, res["ok"]?.jsonPrimitive?.content?.toBooleanStrictOrNull())
    }

    @Test
    fun testServer_emptyAnswerStore() {
        answerStore.nextResult = NextResult(ok = false, reason = "empty")
        fun getReq(path: String): JsonObject {
            val req = Request.Builder().url("http://127.0.0.1:$testPort$path").get().build()
            client.newCall(req).execute().use { response ->
                assertEquals(200, response.code)
                return Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            }
        }

        val res = getReq("/next")
        assertEquals(false, res["ok"]?.jsonPrimitive?.boolean)
        assertEquals("empty", res["reason"]?.jsonPrimitive?.content)
    }

    @Test
    fun testServer_malformedRequest() {
        // Just open socket and send junk
        val s = java.net.Socket("127.0.0.1", testPort)
        s.outputStream.write("JUNK\r\n\r\n".toByteArray())
        s.outputStream.flush()
        
        val scanner = java.util.Scanner(s.inputStream)
        val response = scanner.nextLine()
        assertTrue(response.startsWith("HTTP/1.1 400"))
        s.close()
    }

    @Test
    fun testServer_concurrentRequests() {
        val latch = CountDownLatch(5)
        val results = mutableListOf<Int>()
        val threads = mutableListOf<Thread>()

        for (i in 0 until 5) {
            val t = thread {
                val req = Request.Builder().url("http://127.0.0.1:$testPort/ping").get().build()
                try {
                    client.newCall(req).execute().use { response ->
                        synchronized(results) { results.add(response.code) }
                    }
                } catch (e: Exception) {
                    // Ignore
                } finally {
                    latch.countDown()
                }
            }
            threads.add(t)
        }

        latch.await(5, TimeUnit.SECONDS)
        assertEquals(5, results.size)
        assertTrue(results.all { it == 200 })
    }
}
