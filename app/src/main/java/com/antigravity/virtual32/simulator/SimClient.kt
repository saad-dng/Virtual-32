package com.antigravity.virtual32.simulator

import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class SimClient(private val host: String, private val port: Int) {
    
    private val pingClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    private val uploadClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS) // Upload timeout 60s
        .build()
        
    private val baseUrl = "http://$host:$port"

    fun ping(): Boolean {
        return try {
            val req = Request.Builder().url("$baseUrl/ping").get().build()
            val resp = pingClient.newCall(req).execute()
            val success = resp.isSuccessful
            resp.close()
            success
        } catch (e: Exception) {
            false
        }
    }

    fun next(): JsonObject? = getJson("/next")
    
    fun repeat(): JsonObject? = getJson("/repeat")

    private fun getJson(path: String): JsonObject? {
        return try {
            val req = Request.Builder().url("$baseUrl$path").get().build()
            val resp = pingClient.newCall(req).execute()
            val body = resp.body?.string()
            resp.close()
            if (resp.isSuccessful && body != null) {
                Json.parseToJsonElement(body).jsonObject
            } else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun uploadWithRetries(jpeg: ByteArray): JsonObject? {
        var attempts = 0
        var backoff = 1000L
        while (attempts < 3) {
            try {
                val body = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "photo.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
                    .build()
                val req = Request.Builder().url("$baseUrl/upload").post(body).build()
                val resp = uploadClient.newCall(req).execute()
                val respStr = resp.body?.string()
                resp.close()
                if (resp.isSuccessful && respStr != null) {
                    return Json.parseToJsonElement(respStr).jsonObject
                }
            } catch (e: Exception) {
                // fall through to retry
            }
            attempts++
            if (attempts < 3) {
                delay(backoff)
                backoff *= 2
            }
        }
        return null
    }
}
