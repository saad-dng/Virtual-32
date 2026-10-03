package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.antigravity.virtual32.receiver.pipeline.SessionManager
import com.antigravity.virtual32.receiver.pipeline.SessionPhotoEntry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class HomeViewModelSessionTest {

    private lateinit var app: Application
    private var fakeTime = 10_000L
    private var submittedPhotos: List<SessionPhotoEntry>? = null
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "open_session.json").delete()

        submittedPhotos = null
        fakeTime = 10_000L

        sessionManager = SessionManager(
            context = app,
            timeSource = { fakeTime },
            onAutoSubmit = { photos ->
                submittedPhotos = photos
            }
        )

        viewModel = HomeViewModel(app, sessionManager)
    }

    @Test
    fun testSessionCardDelete() = runBlocking {
        sessionManager.addPhoto("/dummy/1.jpg", null, 12, 0)
        sessionManager.addPhoto("/dummy/2.jpg", null, 12, 0)
        sessionManager.addPhoto("/dummy/3.jpg", null, 12, 0)

        assertEquals(3, viewModel.sessionUiState.value.photos.size)

        // Delete photo at index 1 ("/dummy/2.jpg")
        viewModel.deletePhoto(1)

        val updated = viewModel.sessionUiState.value.photos
        assertEquals(2, updated.size)
        assertEquals("/dummy/1.jpg", updated[0].cachePath)
        assertEquals("/dummy/3.jpg", updated[1].cachePath)
    }

    @Test
    fun testSessionCardReorder() = runBlocking {
        sessionManager.addPhoto("/dummy/1.jpg", null, 12, 0)
        sessionManager.addPhoto("/dummy/2.jpg", null, 12, 0)
        sessionManager.addPhoto("/dummy/3.jpg", null, 12, 0)

        // Move photo 2 earlier (index 2 -> index 1)
        viewModel.moveEarlier(2)
        var photos = viewModel.sessionUiState.value.photos
        assertEquals("/dummy/1.jpg", photos[0].cachePath)
        assertEquals("/dummy/3.jpg", photos[1].cachePath)
        assertEquals("/dummy/2.jpg", photos[2].cachePath)

        // Move photo 1 later (index 0 -> index 1)
        viewModel.moveLater(0)
        photos = viewModel.sessionUiState.value.photos
        assertEquals("/dummy/3.jpg", photos[0].cachePath)
        assertEquals("/dummy/1.jpg", photos[1].cachePath)
        assertEquals("/dummy/2.jpg", photos[2].cachePath)
    }

    @Test
    fun testSessionCardAnalyzeNow() = runBlocking {
        sessionManager.addPhoto("/dummy/1.jpg", null, 12, 0)
        sessionManager.addPhoto("/dummy/2.jpg", null, 12, 0)
        assertEquals(2, viewModel.sessionUiState.value.photos.size)

        viewModel.analyzeNow()

        // After analyzeNow, open session is frozen/cleared
        assertEquals(0, viewModel.sessionUiState.value.photos.size)
        assertFalse(viewModel.sessionUiState.value.open)
    }

    @Test
    fun testSessionCardCancel() = runBlocking {
        sessionManager.addPhoto("/dummy/1.jpg", null, 12, 0)
        assertEquals(1, viewModel.sessionUiState.value.photos.size)

        viewModel.cancelSession()

        assertEquals(0, viewModel.sessionUiState.value.photos.size)
        assertFalse(viewModel.sessionUiState.value.open)
    }

    @Test
    fun testSessionCardCountdown() = runBlocking {
        fakeTime = 10_000L
        sessionManager.addPhoto("/dummy/1.jpg", null, 12, autoSubmitSec = 30)

        // Immediately remaining = 30s
        val remaining0 = viewModel.getAutoSubmitCountdown(fakeTime, 30)
        assertEquals(30, remaining0)

        // After 10s elapsed
        fakeTime += 10_000L
        val remaining10 = viewModel.getAutoSubmitCountdown(fakeTime, 30)
        assertEquals(20, remaining10)

        // After 35s elapsed (expired)
        fakeTime += 25_000L
        val remaining35 = viewModel.getAutoSubmitCountdown(fakeTime, 30)
        assertEquals(0, remaining35)
    }
}
