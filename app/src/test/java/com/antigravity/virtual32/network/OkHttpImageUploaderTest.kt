package com.antigravity.virtual32.network

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

class OkHttpImageUploaderTest {

    private lateinit var server: MockWebServer
    private lateinit var uploader: OkHttpImageUploader
    private val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()

        val fastClient = OkHttpClient.Builder()
            .connectTimeout(500, TimeUnit.MILLISECONDS)
            .readTimeout(500, TimeUnit.MILLISECONDS)
            .writeTimeout(500, TimeUnit.MILLISECONDS)
            .retryOnConnectionFailure(false)
            .build()

        uploader = OkHttpImageUploader(fastClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun testUpload_200Success() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(200)
                .setBody("{\"status\":\"ok\",\"description\":\"scene processed\"}")
        )

        val result = uploader.uploadFrame(server.hostName, server.port, dummyJpeg)
        assertTrue(result is NetworkResult.Success)
        val success = result as NetworkResult.Success
        assertEquals(200, success.code)
        assertTrue(success.body.contains("scene processed"))

        val recordedRequest = server.takeRequest()
        assertEquals("/upload", recordedRequest.path)
        assertEquals("POST", recordedRequest.method)
        assertTrue(recordedRequest.headers["Content-Type"]?.startsWith("multipart/form-data") == true)
    }

    @Test
    fun testUpload_422ValidationError() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("{\"error\":\"Image blur detected\"}")
        )

        val result = uploader.uploadFrame(server.hostName, server.port, dummyJpeg)
        assertTrue(result is NetworkResult.ClientError422)
        val error = result as NetworkResult.ClientError422
        assertEquals(422, error.code)
        assertTrue(error.message.contains("Image blur detected"))
    }

    @Test
    fun testUpload_500ServerError() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error")
        )

        val result = uploader.uploadFrame(server.hostName, server.port, dummyJpeg)
        assertTrue(result is NetworkResult.ServerError5xx)
        val error = result as NetworkResult.ServerError5xx
        assertEquals(500, error.code)
    }

    @Test
    fun testUpload_MalformedOrUnexpectedCode() = runBlocking {
        server.enqueue(
            MockResponse()
                .setResponseCode(418)
                .setBody("I'm a teapot")
        )

        val result = uploader.uploadFrame(server.hostName, server.port, dummyJpeg)
        assertTrue(result is NetworkResult.MalformedResponse)
        val malformed = result as NetworkResult.MalformedResponse
        assertEquals(418, malformed.code)
    }

    @Test
    fun testUpload_Timeout() = runBlocking {
        server.enqueue(
            MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
        )

        val result = uploader.uploadFrame(server.hostName, server.port, dummyJpeg)
        assertTrue(result is NetworkResult.Timeout)
    }

    @Test
    fun testResolveEndpointUrl_handlesComplexInputs() {
        assertEquals("http://192.168.1.10:5000/upload", OkHttpImageUploader.resolveEndpointUrl("192.168.1.10", 5000))
        assertEquals("http://192.168.1.10:5000/upload", OkHttpImageUploader.resolveEndpointUrl("192.168.1.10:5000", 5000))
        assertEquals("http://192.168.1.10:8080/upload", OkHttpImageUploader.resolveEndpointUrl("http://192.168.1.10:8080", 5000))
        assertEquals("http://192.168.1.10:5000/custom", OkHttpImageUploader.resolveEndpointUrl("192.168.1.10:5000/custom", 5000))
    }

    @Test
    fun testConnection_successAndFailure() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{\"status\":\"online\"}"))
        val (success, message) = uploader.testConnection(server.hostName, server.port)
        assertTrue(success)
        assertTrue(message.contains("online"))

        val (fail, _) = uploader.testConnection("127.0.0.1", 65432)
        assertTrue(!fail)
    }
}
