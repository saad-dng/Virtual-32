package com.antigravity.virtual32.receiver

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.ServerSocket
import java.net.Socket
import java.util.Collections

data class NetworkInterfaceDetail(
    val ip: String,
    val name: String,
    val type: String,
    val isPrimary: Boolean = false
)

/**
 * Embedded HTTP server for Receiver.
 * Receives multipart JPEG uploads from Phone 2 on /upload, invokes Gemini Vision,
 * and responds with HTTP 200 (or HTTP 422 on invalid payload).
 */
class ReceiverHttpServer(
    private val port: Int = 5000,
    private val context: Context? = null,
    private val onFrameReceived: suspend (jpegBytes: ByteArray, clientIp: String) -> String
) {
    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private var wakeLock: android.os.PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null

    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

    private val _lastClientIp = MutableStateFlow("None")
    val lastClientIp: StateFlow<String> = _lastClientIp.asStateFlow()

    companion object {
        private const val TAG = "ReceiverHttpServer"

        /**
         * Returns all active local IPv4 addresses classified by network type.
         */
        fun getAvailableInterfaces(context: Context? = null): List<NetworkInterfaceDetail> {
            val list = mutableListOf<NetworkInterfaceDetail>()
            try {
                val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
                for (intf in interfaces) {
                    if (!intf.isUp || intf.isLoopback) continue
                    val ifName = intf.name.lowercase()
                    val addrs = Collections.list(intf.inetAddresses)
                    for (addr in addrs) {
                        if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                            val ip = addr.hostAddress ?: continue
                            if (ip.isBlank() || ip.startsWith("127.")) continue

                            val type = when {
                                ifName.startsWith("wlan") || ifName.contains("wifi") -> "Wi-Fi"
                                ifName.startsWith("ap") || ifName.startsWith("softap") || ifName.startsWith("swlan") || ifName.startsWith("rndis") || ip.startsWith("192.168.43.") -> "Mobile Hotspot"
                                ifName.startsWith("rmnet") || ifName.startsWith("ccmni") || ifName.startsWith("pdp") -> "Cellular Data"
                                ip.startsWith("192.168.") || ip.startsWith("10.") || ip.startsWith("172.") -> "Local Network"
                                else -> "Network (${intf.name})"
                            }
                            list.add(NetworkInterfaceDetail(ip = ip, name = intf.name, type = type))
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Error enumerating interfaces: ${e.message}")
            }
            return list
        }

        /**
         * Resolves the primary local IPv4 address (prioritizing Wi-Fi, then Hotspot, then Local Network).
         * Explicitly avoids mobile carrier NAT IPs (rmnet/cellular) unless nothing else exists.
         */
        fun getLocalIpAddress(context: Context): String {
            val interfaces = getAvailableInterfaces(context)

            // Priority 1: Active Wi-Fi
            interfaces.firstOrNull { it.type == "Wi-Fi" }?.let { return it.ip }

            // Priority 2: Active Mobile Hotspot
            interfaces.firstOrNull { it.type == "Mobile Hotspot" }?.let { return it.ip }

            // Priority 3: Any private subnet IP (192.168.*.* or 172.16-31.*.* or 10.*.*.*) not cellular
            interfaces.firstOrNull { it.type == "Local Network" }?.let { return it.ip }

            // Priority 4: Any non-cellular IP
            interfaces.firstOrNull { it.type != "Cellular Data" }?.let { return it.ip }

            // Fallback: If on hotspot without interface name detection, standard Android hotspot gateway
            return interfaces.firstOrNull()?.ip ?: "192.168.43.1"
        }
    }

    fun start() {
        if (_isRunning.value) return

        // Acquire partial wake lock & wifi lock to keep socket alive if screen is locked
        context?.let { ctx ->
            try {
                val powerManager = ctx.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
                wakeLock = powerManager?.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "Virtual32:ReceiverServer")?.apply {
                    setReferenceCounted(false)
                    acquire(2 * 60 * 60 * 1000L) // 2 hours max
                }

                val wifiManager = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "Virtual32:WifiServer")?.apply {
                    setReferenceCounted(false)
                    acquire()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Could not acquire wake/wifi locks: ${e.message}")
            }
        }

        serverJob = scope.launch {
            try {
                // Bind explicitly to 0.0.0.0 (all network interfaces: Wi-Fi, Hotspot, USB, Loopback)
                serverSocket = ServerSocket(port, 50, java.net.InetAddress.getByName("0.0.0.0")).apply {
                    reuseAddress = true
                }
                _isRunning.value = true
                Log.i(TAG, "Receiver HTTP server started on 0.0.0.0:$port")

                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch {
                        handleClient(clientSocket)
                    }
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.e(TAG, "Server socket error", e)
                }
            } finally {
                _isRunning.value = false
            }
        }
    }

    fun stop() {
        _isRunning.value = false
        serverJob?.cancel()
        runCatching { serverSocket?.close() }
        serverSocket = null

        runCatching {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            wakeLock = null
            if (wifiLock?.isHeld == true) wifiLock?.release()
            wifiLock = null
        }
        Log.i(TAG, "Receiver HTTP server stopped")
    }

    private suspend fun handleClient(socket: Socket) = withContext(Dispatchers.IO) {
        val clientIp = socket.inetAddress?.hostAddress ?: "Unknown"
        _lastClientIp.value = clientIp

        try {
            socket.soTimeout = 15000
            val input = BufferedInputStream(socket.getInputStream())
            val output = BufferedOutputStream(socket.getOutputStream())

            // Read HTTP headers
            val headerBytes = ByteArrayOutputStream()
            var prev = -1
            var prevPrev = -1
            var prevPrevPrev = -1

            while (true) {
                val b = input.read()
                if (b == -1) break
                headerBytes.write(b)
                if (prevPrevPrev == '\r'.code && prevPrev == '\n'.code && prev == '\r'.code && b == '\n'.code) {
                    break
                }
                prevPrevPrev = prevPrev
                prevPrev = prev
                prev = b
            }

            val headerString = headerBytes.toString("UTF-8")
            val lines = headerString.split("\r\n")
            val requestLine = lines.firstOrNull().orEmpty()
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

            Log.d(TAG, "Request: $requestLine from $clientIp")

            if (requestLine.startsWith("GET /status") || requestLine.startsWith("GET / ")) {
                val body = "{\"status\":\"online\",\"role\":\"Receiver\"}"
                sendResponse(output, 200, "OK", "application/json", body.toByteArray())
                return@withContext
            }

            if (!requestLine.startsWith("POST /upload")) {
                sendResponse(output, 404, "Not Found", "text/plain", "Not Found".toByteArray())
                return@withContext
            }

            val contentLength = headers["content-length"]?.toIntOrNull() ?: 0
            val contentType = headers["content-type"].orEmpty()

            if (contentLength <= 0 || !contentType.contains("boundary=")) {
                val errBody = "{\"status\":\"error\",\"message\":\"Missing boundary or payload\"}"
                sendResponse(output, 422, "Unprocessable Entity", "application/json", errBody.toByteArray())
                return@withContext
            }

            val rawBoundary = contentType.substringAfter("boundary=").substringBefore(";").trim().removeSurrounding("\"")
            val boundary = rawBoundary

            // Read the full body
            val bodyData = ByteArray(contentLength)
            var bytesRead = 0
            while (bytesRead < contentLength) {
                val read = input.read(bodyData, bytesRead, contentLength - bytesRead)
                if (read == -1) break
                bytesRead += read
            }

            // Extract JPEG bytes from multipart body
            val jpegBytes = extractJpegFromMultipart(bodyData, boundary)

            if (jpegBytes != null && jpegBytes.isNotEmpty()) {
                Log.d(TAG, "Extracted JPEG: ${jpegBytes.size} bytes from $clientIp")

                // Process frame (call Gemini Vision + speak via TTS)
                val analysisResult = onFrameReceived(jpegBytes, clientIp)

                val responseJson = "{\"status\":\"ok\",\"analysis\":${quoteJson(analysisResult)}}"
                sendResponse(output, 200, "OK", "application/json", responseJson.toByteArray())
            } else {
                Log.w(TAG, "Failed extracting JPEG from multipart body")
                val errJson = "{\"status\":\"error\",\"message\":\"Validation failed: invalid or missing image part\"}"
                sendResponse(output, 422, "Unprocessable Entity", "application/json", errJson.toByteArray())
            }
        } catch (e: Exception) {
            Log.e(TAG, "Client socket error: ${e.message}")
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun extractJpegFromMultipart(body: ByteArray, boundary: String): ByteArray? {
        val boundaryBytes = ("--$boundary").toByteArray()
        val endBoundaryBytes = ("\r\n\r\n").toByteArray()

        var startIndex = indexOf(body, boundaryBytes, 0)
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

    private fun sendResponse(
        out: BufferedOutputStream,
        statusCode: Int,
        statusText: String,
        contentType: String,
        body: ByteArray
    ) {
        val responseHeaders = "HTTP/1.1 $statusCode $statusText\r\n" +
                "Content-Type: $contentType\r\n" +
                "Content-Length: ${body.size}\r\n" +
                "Connection: close\r\n\r\n"
        out.write(responseHeaders.toByteArray(Charsets.UTF_8))
        out.write(body)
        out.flush()
    }

    private fun quoteJson(string: String): String {
        val sb = StringBuilder("\"")
        for (c in string) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (c.code in 0x00..0x1f) {
                        sb.append(String.format("\\u%04x", c.code))
                    } else {
                        sb.append(c)
                    }
                }
            }
        }
        sb.append("\"")
        return sb.toString()
    }
}
