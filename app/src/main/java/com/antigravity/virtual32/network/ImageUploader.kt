package com.antigravity.virtual32.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

interface ImageUploader {
    suspend fun uploadFrame(
        serverIp: String,
        serverPort: Int,
        jpegBytes: ByteArray
    ): NetworkResult

    suspend fun testConnection(
        serverIp: String,
        serverPort: Int
    ): Pair<Boolean, String>
}

/**
 * Real OkHttp implementation for multipart/form-data JPEG upload.
 * Posts to Phone 1's receiver server over local hotspot / Wi-Fi.
 */
class OkHttpImageUploader(
    private val client: OkHttpClient = createDefaultClient()
) : ImageUploader {

    companion object {
        private const val TAG = "OkHttpImageUploader"

        fun createDefaultClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
        }

        fun resolveEndpointUrl(serverIp: String, serverPort: Int, path: String = "/upload"): String {
            val trimmed = serverIp.trim()
                .removePrefix("http://")
                .removePrefix("https://")
                .trimEnd('/')

            val hostAndPort: String
            val finalPath: String

            if (trimmed.contains("/")) {
                hostAndPort = trimmed.substringBefore("/")
                val extractedPath = "/" + trimmed.substringAfter("/").trimStart('/')
                finalPath = if (extractedPath.isBlank() || extractedPath == "/") path else extractedPath
            } else {
                hostAndPort = trimmed
                finalPath = path
            }

            val host: String
            val port: Int

            if (hostAndPort.contains(":")) {
                host = hostAndPort.substringBefore(":").trim()
                port = hostAndPort.substringAfter(":").trim().toIntOrNull() ?: serverPort
            } else {
                host = hostAndPort.trim()
                port = serverPort
            }

            return "http://$host:$port$finalPath"
        }
    }

    override suspend fun testConnection(
        serverIp: String,
        serverPort: Int
    ): Pair<Boolean, String> = withContext(Dispatchers.IO) {
        val testUrl = resolveEndpointUrl(serverIp, serverPort, "/status")
        Log.d(TAG, "Testing connection to: $testUrl")

        val testClient = client.newBuilder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(3, TimeUnit.SECONDS)
            .build()

        val request = Request.Builder()
            .url(testUrl)
            .get()
            .build()

        try {
            testClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    Log.i(TAG, "Connection successful to Phone 1: $body")
                    Pair(true, "Connected! Phone 1 Receiver is online and ready (HTTP ${response.code}).")
                } else {
                    Pair(false, "Phone 1 responded with error HTTP ${response.code}")
                }
            }
        } catch (e: ConnectException) {
            Pair(false, "Cannot connect: Connection refused. Ensure Phone 1 server switch is ON and both phones share the same Wi-Fi/Hotspot.")
        } catch (e: SocketTimeoutException) {
            Pair(false, "Connection timed out after 3s. Check IP address and Wi-Fi signal.")
        } catch (e: UnknownHostException) {
            Pair(false, "Unknown host address. Please verify the IP address numbers.")
        } catch (e: Exception) {
            Pair(false, "Connection test failed: ${e.message ?: "Unknown error"}")
        }
    }

    override suspend fun uploadFrame(
        serverIp: String,
        serverPort: Int,
        jpegBytes: ByteArray
    ): NetworkResult = withContext(Dispatchers.IO) {
        val url = resolveEndpointUrl(serverIp, serverPort, "/upload")
        Log.d(TAG, "Initiating multipart upload to: $url (${jpegBytes.size} bytes)")

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "image",
                filename = "frame.jpg",
                body = jpegBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string().orEmpty()
                val code = response.code

                Log.d(TAG, "Response received: HTTP $code, length: ${bodyString.length}")

                when (code) {
                    200 -> NetworkResult.Success(code, bodyString)
                    422 -> NetworkResult.ClientError422(code, bodyString.ifEmpty { "Validation error (HTTP 422)" })
                    in 500..599 -> NetworkResult.ServerError5xx(code, bodyString.ifEmpty { "Server error (HTTP $code)" })
                    else -> {
                        // Check if response is malformed or unexpected code
                        NetworkResult.MalformedResponse(code, "Unexpected response status: HTTP $code")
                    }
                }
            }
        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Upload timed out: ${e.message}")
            NetworkResult.Timeout("Request timed out after 10s")
        } catch (e: ConnectException) {
            Log.w(TAG, "Server unreachable at $url: ${e.message}")
            NetworkResult.Unreachable("Failed connecting to $url")
        } catch (e: UnknownHostException) {
            Log.w(TAG, "No network route to host: ${e.message}")
            NetworkResult.NoNetwork("No route to host / hotspot disconnected")
        } catch (e: IOException) {
            Log.e(TAG, "I/O error during upload: ${e.message}", e)
            NetworkResult.UnknownError("I/O error: ${e.message ?: "Unknown I/O exception"}")
        } catch (e: Throwable) {
            Log.e(TAG, "Unexpected upload exception: ${e.message}", e)
            NetworkResult.UnknownError(e.message ?: "Unexpected failure")
        }
    }
}
