package com.antigravity.virtual32.network

/**
 * Sealed representation of all network response cases for Phone 1 Termux communication.
 * Distinguishes HTTP 200, 422, 5xx, timeouts, unreachable hosts, no network, and malformed bodies.
 */
sealed class NetworkResult {
    /** HTTP 200 OK — Termux server received and accepted frame */
    data class Success(val code: Int, val body: String) : NetworkResult()

    /** HTTP 422 Unprocessable Entity — Validation failure on server */
    data class ClientError422(val code: Int = 422, val message: String) : NetworkResult()

    /** HTTP 5xx — Server-side crash or internal error */
    data class ServerError5xx(val code: Int, val message: String) : NetworkResult()

    /** Request timeout — Server did not respond within deadline */
    data class Timeout(val message: String) : NetworkResult()

    /** Host unreachable — Wrong IP or server daemon down */
    data class Unreachable(val message: String) : NetworkResult()

    /** No network / hotspot disconnected — Device has no LAN route */
    data class NoNetwork(val message: String) : NetworkResult()

    /** Malformed or unexpected response body */
    data class MalformedResponse(val code: Int, val message: String) : NetworkResult()

    /** Generic unexpected network error */
    data class UnknownError(val message: String) : NetworkResult()
}
