package com.antigravity.virtual32.receiver.server

import android.util.Log
import com.antigravity.virtual32.data.AnswerStore
import com.antigravity.virtual32.receiver.pipeline.PhotoPipeline
import com.antigravity.virtual32.util.IpDiscovery
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketTimeoutException
import com.antigravity.virtual32.receiver.service.ReceiverState

import com.antigravity.virtual32.receiver.pipeline.SessionManager
import com.antigravity.virtual32.receiver.pipeline.SessionInfo
import com.antigravity.virtual32.receiver.pipeline.AddPhotoResult
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.util.GalleryWriter
import com.antigravity.virtual32.util.PhotoCache
import java.io.File

class ReceiverHttpServer(
    private var port: Int = 5000,
    private val pipeline: PhotoPipeline,
    private val answerStore: AnswerStore,
    private val logBuffer: LogBuffer,
    private val sessionManager: SessionManager? = null,
    private val galleryWriter: GalleryWriter? = null,
    private val photoCache: PhotoCache? = null,
    private val settingsRepo: SettingsRepository? = null
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    
    private val _state = MutableStateFlow(ServerState(port = port))
    val state: StateFlow<ServerState> = _state.asStateFlow()
    
    private var requestCount = 0
    private var lastSeenMs = 0L

    companion object {
        private const val TAG = "ReceiverHttpServer"
        private const val MAX_HEADER_SIZE = 16 * 1024
        private const val MAX_BODY_SIZE = 8 * 1024 * 1024 // 8 MB
    }

    fun start() {
        if (_state.value.isRunning) return
        
        serverJob = scope.launch {
            try {
                var bound = false
                try {
                    serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0")).apply { reuseAddress = true }
                    bound = true
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to bind to $port, trying 8080", e)
                    port = 8080
                    serverSocket = ServerSocket(port, 50, InetAddress.getByName("0.0.0.0")).apply { reuseAddress = true }
                    bound = true
                }
                
                if (bound) {
                    updateState { it.copy(isRunning = true, boundIp = IpDiscovery.getBestIpAddress(), port = port, lastError = null) }
                    Log.i(TAG, "Server started on $port")
                    while (isActive) {
                        val clientSocket = serverSocket?.accept() ?: break
                        launch {
                            handleClient(clientSocket)
                        }
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Server socket error", e)
                    updateState { it.copy(isRunning = false, lastError = e.message) }
                }
            } finally {
                updateState { it.copy(isRunning = false) }
            }
        }
    }

    fun stop() {
        if (!_state.value.isRunning) return
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null
        updateState { it.copy(isRunning = false) }
        Log.i(TAG, "Server stopped")
    }

    private fun updateState(updater: (ServerState) -> ServerState) {
        _state.value = updater(_state.value)
    }

    private fun markSeen() {
        lastSeenMs = System.currentTimeMillis()
        requestCount++
        updateState { it.copy(lastSeenMs = lastSeenMs, requestCount = requestCount) }
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var reqMethod = ""
        var reqPath = ""
        var resStatus = 500

        try {
            socket.soTimeout = 15000
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            val headerBytes = ByteArrayOutputStream()
            var prevPrevPrev = -1
            var prevPrev = -1
            var prev = -1
            var bytesRead = 0

            while (true) {
                if (bytesRead > MAX_HEADER_SIZE) {
                    resStatus = 431
                    sendJsonResponse(output, 431, buildJsonObject { put("error", "Headers too large") })
                    return@withContext
                }
                val b = input.read()
                if (b == -1) break
                headerBytes.write(b)
                bytesRead++
                
                if (prevPrevPrev == '\r'.code && prevPrev == '\n'.code && prev == '\r'.code && b == '\n'.code) {
                    break
                }
                prevPrevPrev = prevPrev
                prevPrev = prev
                prev = b
            }

            if (headerBytes.size() == 0) return@withContext
            markSeen()

            val headerString = headerBytes.toString("UTF-8")
            val lines = headerString.split("\r\n")
            val requestLine = lines.firstOrNull().orEmpty()
            
            val reqParts = requestLine.split(" ")
            if (reqParts.size < 2) {
                resStatus = 400
                sendJsonResponse(output, 400, buildJsonObject { put("error", "Malformed request line") })
                return@withContext
            }
            reqMethod = reqParts[0]
            reqPath = reqParts[1]

            val headers = mutableMapOf<String, String>()
            for (i in 1 until lines.size) {
                val line = lines[i]
                val idx = line.indexOf(':')
                if (idx != -1) {
                    val key = line.substring(0, idx).trim().lowercase()
                    val value = line.substring(idx + 1).trim()
                    headers[key] = value
                }
            }

            resStatus = handleRoute(reqMethod, reqPath, headers, input, output)

        } catch (e: SocketTimeoutException) {
            Log.w(TAG, "Socket timeout: ${e.message}")
            resStatus = 408
        } catch (e: Exception) {
            Log.e(TAG, "Client error: ${e.message}", e)
            resStatus = 500
        } finally {
            val duration = System.currentTimeMillis() - startTime
            if (reqMethod.isNotEmpty()) {
                logBuffer.addHttpLog(reqMethod, reqPath, resStatus, duration)
            }
            runCatching { socket.close() }
        }
    }

    private suspend fun handleRoute(
        method: String,
        path: String,
        headers: Map<String, String>,
        input: BufferedInputStream,
        output: BufferedOutputStream
    ): Int {
        if (method == "GET") {
            return when (path) {
                "/ping", "/status" -> {
                    val openPages = sessionManager?.getSessionInfoBlocking()?.pages ?: 0
                    val resp = buildJsonObject {
                        put("ok", true)
                        put("app", "virtual32")
                        put("answers", answerStore.count)
                        put("cursor", answerStore.cursor)
                        put("busy", false)
                        put("pages", openPages)
                    }
                    sendJsonResponse(output, 200, resp)
                    200
                }
                "/session" -> {
                    val info = sessionManager?.getSessionInfo() ?: SessionInfo(open = false, pages = 0, idleMs = 0L)
                    val resp = buildJsonObject {
                        put("open", info.open)
                        put("pages", info.pages)
                        put("idleMs", info.idleMs)
                    }
                    sendJsonResponse(output, 200, resp)
                    200
                }
                "/next" -> {
                    val res = answerStore.next()
                    ReceiverState.updateLastResult(res)
                    sendJsonResponse(output, 200, encodeNextResult(res))
                    200
                }
                "/repeat" -> {
                    val res = answerStore.repeat()
                    ReceiverState.updateLastResult(res)
                    sendJsonResponse(output, 200, encodeNextResult(res))
                    200
                }
                "/reset" -> {
                    val res = answerStore.reset()
                    ReceiverState.updateLastResult(res)
                    sendJsonResponse(output, 200, encodeNextResult(res))
                    200
                }
                else -> {
                    sendJsonResponse(output, 404, buildJsonObject { put("error", "Not found") })
                    404
                }
            }
        } else if (method == "POST") {
            return when (path) {
                "/upload" -> handleUpload(headers, input, output)
                "/session/photo" -> handleSessionPhoto(headers, input, output)
                "/session/finish" -> handleSessionFinish(output)
                "/session/cancel" -> handleSessionCancel(output)
                else -> {
                    sendJsonResponse(output, 404, buildJsonObject { put("error", "Not found") })
                    404
                }
            }
        } else {
            sendJsonResponse(output, 405, buildJsonObject { put("error", "Method Not Allowed") })
            return 405
        }
    }

    private fun encodeNextResult(res: com.antigravity.virtual32.data.NextResult): JsonObject {
        return buildJsonObject {
            put("ok", res.ok)
            if (res.reason != null) put("reason", res.reason)
            if (res.q != null) put("q", res.q)
            if (res.of != null) put("of", res.of)
            if (res.choice != null) put("choice", res.choice)
            if (res.blinks != null) put("blinks", res.blinks)
            if (res.end != null) put("end", res.end)
        }
    }

    private fun extractJpegBytes(
        headers: Map<String, String>,
        input: BufferedInputStream,
        output: BufferedOutputStream
    ): Pair<ByteArray?, Int> {
        val contentLengthStr = headers["content-length"]
        val contentType = headers["content-type"].orEmpty()
        val isChunked = headers["transfer-encoding"]?.lowercase() == "chunked"

        val bodyData: ByteArray
        if (isChunked) {
            bodyData = readChunkedBody(input)
        } else {
            val contentLength = contentLengthStr?.toIntOrNull() ?: 0
            if (contentLength > MAX_BODY_SIZE) {
                sendJsonResponse(output, 413, buildJsonObject { put("status", "error"); put("reason", "Payload Too Large") })
                return null to 413
            }
            bodyData = readExact(input, contentLength)
        }

        if (bodyData.size > MAX_BODY_SIZE) {
            sendJsonResponse(output, 413, buildJsonObject { put("status", "error"); put("reason", "Payload Too Large") })
            return null to 413
        }

        var jpegBytes: ByteArray? = null
        if (contentType.contains("multipart/form-data")) {
            val boundary = contentType.substringAfter("boundary=").substringBefore(";").trim().removeSurrounding("\"")
            jpegBytes = extractJpegFromMultipart(bodyData, boundary)
        } else if (contentType.contains("image/jpeg")) {
            jpegBytes = bodyData
        }

        if (jpegBytes == null || jpegBytes.size < 2 || jpegBytes[0] != 0xFF.toByte() || jpegBytes[1] != 0xD8.toByte()) {
            sendJsonResponse(output, 400, buildJsonObject { put("status", "error"); put("reason", "bad_image") })
            return null to 400
        }

        return jpegBytes to 200
    }

    private suspend fun handleUpload(
        headers: Map<String, String>,
        input: BufferedInputStream,
        output: BufferedOutputStream
    ): Int {
        val (jpegBytes, status) = extractJpegBytes(headers, input, output)
        if (jpegBytes == null) return status

        return try {
            val result = withTimeout(45000L) {
                pipeline.processPhoto(jpegBytes)
            }
            sendRawJsonResponse(output, 200, result)
            200
        } catch (e: TimeoutCancellationException) {
            sendJsonResponse(output, 200, buildJsonObject { put("status", "error"); put("reason", "timeout") })
            200
        } catch (e: Exception) {
            sendJsonResponse(output, 200, buildJsonObject { put("status", "error"); put("reason", "ai_failed") })
            200
        }
    }

    private suspend fun handleSessionPhoto(
        headers: Map<String, String>,
        input: BufferedInputStream,
        output: BufferedOutputStream
    ): Int {
        val (jpegBytes, status) = extractJpegBytes(headers, input, output)
        if (jpegBytes == null) return status

        val settings = settingsRepo?.getSettings()
        val saveToGallery = settings?.saveToGallery ?: true
        val batchIdStr = java.util.UUID.randomUUID().toString().substring(0, 8)
        
        // Save to gallery BEFORE anything else
        val galleryUri = if (saveToGallery) galleryWriter?.savePhoto(jpegBytes, "ESP", batchIdStr) else null
        val cachePath = photoCache?.saveInternal(jpegBytes, "s_${batchIdStr}.jpg") ?: ""

        val maxPages = settings?.maxSessionPages ?: 12
        val autoSubmitSec = settings?.sessionAutoSubmitSec ?: 0

        val res = sessionManager?.addPhoto(cachePath, galleryUri, maxPages, autoSubmitSec)
            ?: AddPhotoResult.Success(1)

        return when (res) {
            is AddPhotoResult.Success -> {
                sendJsonResponse(output, 200, buildJsonObject {
                    put("ok", true)
                    put("pages", res.pages)
                })
                200
            }
            is AddPhotoResult.SessionFull -> {
                sendJsonResponse(output, 409, buildJsonObject {
                    put("ok", false)
                    put("reason", "session_full")
                    put("max", res.max)
                })
                409
            }
        }
    }

    private suspend fun handleSessionFinish(output: BufferedOutputStream): Int {
        val frozen = sessionManager?.freezeSession() ?: emptyList()
        if (frozen.isEmpty()) {
            sendJsonResponse(output, 200, buildJsonObject {
                put("status", "error")
                put("reason", "empty_session")
            })
            return 200
        }

        val n = frozen.size
        val jpegs = frozen.map { File(it.cachePath).readBytes() }
        val paths = frozen.map { it.cachePath }
        val uris = frozen.mapNotNull { it.galleryUri }

        val timeoutMs = minOf(150_000L, 45_000L + 8_000L * n)
        return try {
            val result = withTimeout(timeoutMs) {
                pipeline.processPhotos(jpegs, "ESP", paths, uris)
            }
            sendRawJsonResponse(output, 200, result)
            200
        } catch (e: TimeoutCancellationException) {
            sendJsonResponse(output, 200, buildJsonObject {
                put("status", "error")
                put("reason", "timeout")
                put("pages", n)
                put("warnings", buildJsonArray {})
            })
            200
        } catch (e: Exception) {
            sendJsonResponse(output, 200, buildJsonObject {
                put("status", "error")
                put("reason", "ai_failed")
                put("pages", n)
                put("warnings", buildJsonArray {})
            })
            200
        }
    }

    private suspend fun handleSessionCancel(output: BufferedOutputStream): Int {
        sessionManager?.cancelSession()
        sendJsonResponse(output, 200, buildJsonObject { put("ok", true) })
        return 200
    }

    private fun readExact(input: BufferedInputStream, length: Int): ByteArray {
        if (length <= 0) return ByteArray(0)
        val buffer = ByteArray(length)
        var totalRead = 0
        while (totalRead < length) {
            val read = input.read(buffer, totalRead, length - totalRead)
            if (read == -1) break
            totalRead += read
        }
        return buffer
    }

    private fun readChunkedBody(input: BufferedInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        while (true) {
            val line = readLine(input)
            if (line.isEmpty()) break
            val chunkSize = line.split(";")[0].trim().toIntOrNull(16) ?: break
            if (chunkSize == 0) {
                // trailing headers
                while (readLine(input).isNotEmpty()) { }
                break
            }
            val chunk = readExact(input, chunkSize)
            out.write(chunk)
            readLine(input) // CRLF after chunk
            
            if (out.size() > MAX_BODY_SIZE) break
        }
        return out.toByteArray()
    }

    private fun readLine(input: BufferedInputStream): String {
        val out = ByteArrayOutputStream()
        var prev = -1
        while (true) {
            val b = input.read()
            if (b == -1) break
            if (prev == '\r'.code && b == '\n'.code) {
                val arr = out.toByteArray()
                return String(arr, 0, arr.size - 1, Charsets.UTF_8)
            }
            out.write(b)
            prev = b
        }
        return String(out.toByteArray(), Charsets.UTF_8)
    }

    private fun extractJpegFromMultipart(body: ByteArray, boundary: String): ByteArray? {
        val boundaryBytes = ("--$boundary").toByteArray()
        val endBoundaryBytes = ("\r\n\r\n").toByteArray()

        val startIndex = indexOf(body, boundaryBytes, 0)
        if (startIndex == -1) return null

        val headerEndIndex = indexOf(body, endBoundaryBytes, startIndex)
        if (headerEndIndex == -1) return null

        val dataStartIndex = headerEndIndex + endBoundaryBytes.size
        val nextBoundaryIndex = indexOf(body, boundaryBytes, dataStartIndex)
        if (nextBoundaryIndex == -1) return null

        val dataEndIndex = nextBoundaryIndex - 2 // Strip preceding \r\n
        if (dataEndIndex <= dataStartIndex) return null

        val jpeg = ByteArray(dataEndIndex - dataStartIndex)
        System.arraycopy(body, dataStartIndex, jpeg, 0, jpeg.size)
        return jpeg
    }

    private fun indexOf(source: ByteArray, target: ByteArray, fromIndex: Int): Int {
        if (fromIndex >= source.size || target.isEmpty()) return -1
        outer@ for (i in fromIndex..source.size - target.size) {
            for (j in target.indices) {
                if (source[i + j] != target[j]) continue@outer
            }
            return i
        }
        return -1
    }

    private fun sendJsonResponse(out: BufferedOutputStream, statusCode: Int, json: JsonObject) {
        val body = json.toString()
        sendRawJsonResponse(out, statusCode, body)
    }

    private fun sendRawJsonResponse(out: BufferedOutputStream, statusCode: Int, jsonBody: String) {
        val statusText = when (statusCode) {
            200 -> "OK"
            400 -> "Bad Request"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            408 -> "Request Timeout"
            413 -> "Payload Too Large"
            422 -> "Unprocessable Entity"
            431 -> "Request Header Fields Too Large"
            else -> "Internal Server Error"
        }
        val bodyBytes = jsonBody.toByteArray(Charsets.UTF_8)
        val responseHeaders = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: application/json\r\n" +
                "Content-Length: ${bodyBytes.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(responseHeaders.toByteArray(Charsets.UTF_8))
        out.write(bodyBytes)
        out.flush()
    }
}
