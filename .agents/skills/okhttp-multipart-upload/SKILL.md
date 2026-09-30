---
name: okhttp-multipart-upload
description: Guidance on implementing OkHttp multipart/form-data HTTP POST requests in Kotlin, with robust response status handling, timeouts, coroutine integration, and hardware status mapping (LED & vibration). Use when implementing image file upload, handling HTTP error cases (200, 422, 5xx), or configuring network clients.
metadata:
  author: Anti-Gravity Engineering
  version: "1.0"
  keywords:
    - okhttp
    - multipart
    - upload
    - networking
    - coroutines
    - response-handler
---

# OkHttp Multipart Upload & Response Handling

Expert guidance for uploading image files (JPEGs) via HTTP POST multipart/form-data over local Wi-Fi / hotspot connections using OkHttp in Kotlin.

## Core Upload Workflow

### 1. Building the Multipart Request
When uploading a captured JPEG frame to the server (e.g. Flask running on Termux):
- Use `MultipartBody.Builder().setType(MultipartBody.FORM)`.
- Use `asRequestBody("image/jpeg".toMediaType())` for byte arrays or file references.
- Pass appropriate form field names (e.g. `"file"`, `"image"`) matching the server contract.

```kotlin
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.concurrent.TimeUnit

class ImageUploadClient(private val client: OkHttpClient) {

    suspend fun uploadFrame(
        serverUrl: String,
        jpegBytes: ByteArray,
        filename: String = "frame.jpg"
    ): NetworkResult = withContext(Dispatchers.IO) {
        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = filename,
                body = jpegBytes.toRequestBody("image/jpeg".toMediaType())
            )
            .build()

        val request = Request.Builder()
            .url(serverUrl)
            .post(requestBody)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    200 -> NetworkResult.Success(response.code, response.body?.string().orEmpty())
                    422 -> NetworkResult.ClientError(response.code, "Validation Failed (HTTP 422)")
                    in 500..599 -> NetworkResult.ServerError(response.code, "Server Error (HTTP ${response.code})")
                    else -> NetworkResult.UnexpectedStatus(response.code, "Unexpected response: ${response.code}")
                }
            }
        } catch (e: java.net.SocketTimeoutException) {
            NetworkResult.Timeout("Request timed out")
        } catch (e: java.net.ConnectException) {
            NetworkResult.Unreachable("Server unreachable at $serverUrl")
        } catch (e: java.net.UnknownHostException) {
            NetworkResult.NoNetwork("No route to host / hotspot disconnected")
        } catch (e: IOException) {
            NetworkResult.NetworkError("I/O error: ${e.message}")
        }
    }
}
```

## Centralized Response Status Handling

Per `agent.md`, all LED-state + vibration logic for network responses must live in **one place** (e.g., `ResponseStatusHandler`), rather than scattered across UI callbacks.

```kotlin
sealed class NetworkResult {
    data class Success(val code: Int, val body: String) : NetworkResult()
    data class ClientError(val code: Int, val message: String) : NetworkResult()
    data class ServerError(val code: Int, val message: String) : NetworkResult()
    data class UnexpectedStatus(val code: Int, val message: String) : NetworkResult()
    data class Timeout(val message: String) : NetworkResult()
    data class Unreachable(val message: String) : NetworkResult()
    data class NoNetwork(val message: String) : NetworkResult()
    data class NetworkError(val message: String) : NetworkResult()
}

enum class LedState {
    GRAY_IDLE,
    RED,
    AMBER_BLINKING,
    BLUE_BLINKING,
    YELLOW
}

data class FeedbackPattern(
    val ledState: LedState,
    val vibrationDurationMs: Long = 0L
)

class ResponseStatusHandler(private val vibratorHelper: VibratorHelper) {
    fun handleResult(result: NetworkResult): FeedbackPattern {
        val pattern = when (result) {
            is NetworkResult.Success -> {
                // GPIO 1 / GPIO 2: HTTP 200 -> stays gray, no vibration
                FeedbackPattern(LedState.GRAY_IDLE, vibrationDurationMs = 0L)
            }
            is NetworkResult.ClientError -> {
                // HTTP 422: turns red, 1 second buzz
                FeedbackPattern(LedState.RED, vibrationDurationMs = 1000L)
            }
            is NetworkResult.ServerError -> {
                // HTTP 5xx: yellow, 500ms buzz
                FeedbackPattern(LedState.YELLOW, vibrationDurationMs = 500L)
            }
            is NetworkResult.Timeout, is NetworkResult.Unreachable -> {
                // Timeout / Unreachable: amber blinking, 2 short buzzes (300ms)
                FeedbackPattern(LedState.AMBER_BLINKING, vibrationDurationMs = 300L)
            }
            is NetworkResult.NoNetwork -> {
                // Hotspot down / no wifi: blue blinking, 200ms buzz
                FeedbackPattern(LedState.BLUE_BLINKING, vibrationDurationMs = 200L)
            }
            else -> {
                FeedbackPattern(LedState.RED, vibrationDurationMs = 500L)
            }
        }

        if (pattern.vibrationDurationMs > 0) {
            vibratorHelper.vibrate(pattern.vibrationDurationMs)
        }
        return pattern
    }
}
```

## Recommended OkHttpClient Configuration

For local hotspot setups between two phones:
- Configure aggressive connect and read timeouts (e.g. 5–10 seconds) so the app detects connection drops quickly without freezing.
- Add an `HttpLoggingInterceptor` in debug builds.

```kotlin
fun createOkHttpClient(): OkHttpClient {
    return OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false) // Hardware simulation should fail fast on drop
        .build()
}
```
