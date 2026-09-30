package com.antigravity.virtual32.receiver

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

class ReceiverHttpServerTest {

    private lateinit var server: ReceiverHttpServer
    private val testPort = 5999
    private var receivedBytes: ByteArray? = null
    private val client = OkHttpClient.Builder()
        .connectTimeout(2, TimeUnit.SECONDS)
        .readTimeout(2, TimeUnit.SECONDS)
        .build()

    @Before
    fun setUp() {
        receivedBytes = null
        server = ReceiverHttpServer(port = testPort) { bytes, _ ->
            receivedBytes = bytes
            "A test scene with a smartphone"
        }
        server.start()
        Thread.sleep(100) // Allow server to bind
    }

    @After
    fun tearDown() {
        server.stop()
    }

    @Test
    fun testServer_statusEndpoint_returns200() {
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/status")
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(200, response.code)
            val body = response.body?.string().orEmpty()
            assertTrue(body.contains("Phone 1 Earbud Brain"))
        }
    }

    @Test
    fun testServer_uploadMultipart_extractsJpegAndReturns200() {
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
            assertTrue(body.contains("A test scene with a smartphone"))
            assertTrue(receivedBytes != null)
            assertEquals(dummyJpeg.size, receivedBytes?.size)
        }
    }

    @Test
    fun testServer_uploadWithoutImage_returns422() {
        val plainBody = "Just plain text without multipart".toRequestBody("text/plain".toMediaType())
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/upload")
            .post(plainBody)
            .build()

        client.newCall(request).execute().use { response ->
            assertEquals(422, response.code)
        }
    }
}
