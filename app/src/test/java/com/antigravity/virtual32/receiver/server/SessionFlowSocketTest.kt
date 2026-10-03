package com.antigravity.virtual32.receiver.server

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.antigravity.virtual32.data.AnswerStore
import com.antigravity.virtual32.data.NextResult
import com.antigravity.virtual32.receiver.ai.RawAnswer
import com.antigravity.virtual32.receiver.pipeline.*
import com.antigravity.virtual32.settings.AppSettings
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.util.PhotoCache
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

@RunWith(RobolectricTestRunner::class)
class SessionFlowSocketTest {
    private val testPort = 5992
    private lateinit var server: ReceiverHttpServer
    private lateinit var sessionManager: SessionManager
    private lateinit var photoCache: PhotoCache
    private lateinit var context: Context

    private var fakeCurrentTime = 1000000L

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private class FakeStore : AnswerStore {
        override val cursor: Int get() = 0
        override val count: Int get() = 0
        override fun next(): NextResult = NextResult(ok = false)
        override fun repeat(): NextResult = NextResult(ok = false)
        override fun reset(): NextResult = NextResult(ok = true)
        override fun replace(newAnswers: List<RawAnswer>) {}
        override fun append(newAnswers: List<RawAnswer>) {}
    }

    private class TestPipeline : PhotoPipeline {
        var delayMs: Long = 0
        var lastReceivedPhotoCount = 0

        override suspend fun processPhoto(jpeg: ByteArray, source: String): String {
            return processPhotos(listOf(jpeg), source)
        }

        override suspend fun processPhotos(
            jpegs: List<ByteArray>,
            source: String,
            cachedPaths: List<String>,
            galleryUris: List<String>
        ): String {
            lastReceivedPhotoCount = jpegs.size
            if (delayMs > 0) delay(delayMs)
            return buildJsonObject {
                put("status", "ok")
                put("count", 10)
                put("batch", 123)
                put("pages", jpegs.size)
                put("warnings", buildJsonArray {
                    add(JsonPrimitive("Q9 missing"))
                })
            }.toString()
        }
    }

    private val pipeline = TestPipeline()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        File(context.filesDir, "open_session.json").delete()
        photoCache = PhotoCache(context)

        sessionManager = SessionManager(
            context = context,
            timeSource = { fakeCurrentTime }
        )

        val settingsRepo = object : SettingsRepository(context) {
            override val settingsFlow = flowOf(AppSettings(maxSessionPages = 2, sessionAutoSubmitSec = 0))
            override suspend fun getSettings(): AppSettings = AppSettings(maxSessionPages = 2, sessionAutoSubmitSec = 0)
        }

        server = ReceiverHttpServer(
            port = testPort,
            pipeline = pipeline,
            answerStore = FakeStore(),
            logBuffer = LogBuffer(),
            sessionManager = sessionManager,
            galleryWriter = null,
            photoCache = photoCache,
            settingsRepo = settingsRepo
        )
        server.start()
        Thread.sleep(150)
    }

    @After
    fun tearDown() {
        server.stop()
        File(context.filesDir, "open_session.json").delete()
    }

    private fun postPhoto(fileName: String = "photo.jpg"): Pair<Int, JsonObject> {
        val dummyJpeg = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0x12, 0x34, 0xFF.toByte(), 0xD9.toByte())
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("image", fileName, dummyJpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()
        val request = Request.Builder()
            .url("http://127.0.0.1:$testPort/session/photo")
            .post(body)
            .build()
        client.newCall(request).execute().use { response ->
            val resBody = response.body?.string().orEmpty()
            return response.code to Json.parseToJsonElement(resBody).jsonObject
        }
    }

    @Test
    fun testSessionFlow_photo_photo_finish() {
        // Photo 1
        val (code1, res1) = postPhoto("p1.jpg")
        assertEquals(200, code1)
        assertEquals(true, res1["ok"]?.jsonPrimitive?.boolean)
        assertEquals(1, res1["pages"]?.jsonPrimitive?.int)

        // Ping check
        val pingReq = Request.Builder().url("http://127.0.0.1:$testPort/ping").get().build()
        client.newCall(pingReq).execute().use { response ->
            assertEquals(200, response.code)
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertEquals(1, json["pages"]?.jsonPrimitive?.int)
        }

        // Photo 2
        val (code2, res2) = postPhoto("p2.jpg")
        assertEquals(200, code2)
        assertEquals(2, res2["pages"]?.jsonPrimitive?.int)

        // GET /session
        val sessionReq = Request.Builder().url("http://127.0.0.1:$testPort/session").get().build()
        client.newCall(sessionReq).execute().use { response ->
            assertEquals(200, response.code)
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertTrue(json["open"]?.jsonPrimitive?.boolean ?: false)
            assertEquals(2, json["pages"]?.jsonPrimitive?.int)
        }

        // Finish
        val finishReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/session/finish")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        client.newCall(finishReq).execute().use { response ->
            assertEquals(200, response.code)
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertEquals("ok", json["status"]?.jsonPrimitive?.content)
            assertEquals(2, json["pages"]?.jsonPrimitive?.int)
            val warnings = json["warnings"]?.jsonArray?.map { it.jsonPrimitive.content }
            assertTrue(warnings?.contains("Q9 missing") == true)
        }
    }

    @Test
    fun testSessionFlow_emptyFinish() {
        val finishReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/session/finish")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        client.newCall(finishReq).execute().use { response ->
            assertEquals(200, response.code)
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertEquals("error", json["status"]?.jsonPrimitive?.content)
            assertEquals("empty_session", json["reason"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun testSessionFlow_sessionFull_409() {
        // maxSessionPages is 2 in our SettingsRepository override
        val (code1, res1) = postPhoto()
        assertEquals(200, code1)
        assertEquals(1, res1["pages"]?.jsonPrimitive?.int)

        val (code2, res2) = postPhoto()
        assertEquals(200, code2)
        assertEquals(2, res2["pages"]?.jsonPrimitive?.int)

        // 3rd photo should exceed max (2) -> 409
        val (code3, res3) = postPhoto()
        assertEquals(409, code3)
        assertFalse(res3["ok"]?.jsonPrimitive?.boolean ?: true)
        assertEquals("session_full", res3["reason"]?.jsonPrimitive?.content)
        assertEquals(2, res3["max"]?.jsonPrimitive?.int)
    }

    @Test
    fun testSessionFlow_cancel() {
        postPhoto()
        val cancelReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/session/cancel")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        client.newCall(cancelReq).execute().use { response ->
            assertEquals(200, response.code)
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertEquals(true, json["ok"]?.jsonPrimitive?.boolean)
        }

        // Check GET /session
        val sessionReq = Request.Builder().url("http://127.0.0.1:$testPort/session").get().build()
        client.newCall(sessionReq).execute().use { response ->
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertFalse(json["open"]?.jsonPrimitive?.boolean ?: true)
            assertEquals(0, json["pages"]?.jsonPrimitive?.int)
        }

        // Finishing should now be empty_session
        val finishReq = Request.Builder()
            .url("http://127.0.0.1:$testPort/session/finish")
            .post(ByteArray(0).toRequestBody(null))
            .build()
        client.newCall(finishReq).execute().use { response ->
            val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
            assertEquals("empty_session", json["reason"]?.jsonPrimitive?.content)
        }
    }

    @Test
    fun testSessionFlow_newPhotoDuringAnalysis_startsFreshSession() {
        postPhoto("first.jpg")
        pipeline.delayMs = 250 // Artificial delay to simulate analysis

        var finishPages = 0
        val t = thread {
            val finishReq = Request.Builder()
                .url("http://127.0.0.1:$testPort/session/finish")
                .post(ByteArray(0).toRequestBody(null))
                .build()
            client.newCall(finishReq).execute().use { response ->
                val json = Json.parseToJsonElement(response.body?.string().orEmpty()).jsonObject
                finishPages = json["pages"]?.jsonPrimitive?.int ?: 0
            }
        }

        Thread.sleep(50) // Wait for finish to freeze the session and start pipeline

        // New photo arrives while analysis of frozen session is in progress
        val (code, res) = postPhoto("fresh.jpg")
        assertEquals(200, code)
        assertEquals(1, res["pages"]?.jsonPrimitive?.int) // Fresh session starts with page 1!

        t.join()
        assertEquals(1, finishPages) // The frozen session had 1 photo
    }

    @Test
    fun testSessionFlow_autoSubmit_withFakeClock() {
        var autoSubmittedCount = 0
        val customManager = SessionManager(
            context = context,
            timeSource = { fakeCurrentTime },
            onAutoSubmit = { photos ->
                autoSubmittedCount = photos.size
            }
        )

        kotlinx.coroutines.runBlocking {
            customManager.addPhoto("/dummy/p1.jpg", null, 12, autoSubmitSec = 10)
            assertEquals(1, customManager.getSessionInfo().pages)

            // Advance clock by 5 seconds (not yet expired)
            fakeCurrentTime += 5000L
            val triggered1 = customManager.checkAutoSubmit(10, fakeCurrentTime)
            assertFalse(triggered1)
            assertEquals(1, customManager.getSessionInfo().pages)

            // Advance clock by another 6 seconds (total 11s >= 10s)
            fakeCurrentTime += 6000L
            val triggered2 = customManager.checkAutoSubmit(10, fakeCurrentTime)
            assertTrue(triggered2)
            assertEquals(0, customManager.getSessionInfo().pages)
            assertEquals(1, autoSubmittedCount)
        }
    }
}
