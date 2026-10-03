package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.data.Batch
import com.antigravity.virtual32.data.RoomAnswerStore
import com.antigravity.virtual32.receiver.ai.RawAnswer
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AnswersViewModelWarningsTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var db: AppDatabase
    private lateinit var answerStore: RoomAnswerStore
    private lateinit var settingsRepo: SettingsRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        answerStore = RoomAnswerStore(db.answerDao())
        settingsRepo = SettingsRepository(app)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        db.close()
    }

    @Test
    fun testWarningHintComputation() {
        // Unreadable photo hint
        val hintUnreadable = AnswersViewModel.computeWarningHint(listOf("Photo 2 unreadable"))
        assertEquals("retake photo 2 and send all again", hintUnreadable)

        // Missing questions gap hint
        val hintMissing = AnswersViewModel.computeWarningHint(listOf("Q9 missing"))
        assertEquals("retake overlapping pages and send all again", hintMissing)

        // Empty warnings
        val hintEmpty = AnswersViewModel.computeWarningHint(emptyList())
        assertNull(hintEmpty)
    }

    @Test
    fun testWarningsBannerDisplayAndDismiss() = runBlocking {
        // Insert a batch with warnings into the database
        val batch = Batch(
            source = "SESSION",
            photoPath = "/p1.jpg",
            galleryUri = null,
            status = "ok",
            provider = "GEMINI",
            model = "gemini-3.8-flash",
            promptName = "default",
            promptHash = "h",
            latencyMs = 500,
            rawResponse = null,
            pageCount = 2,
            warnings = "[\"Photo 2 unreadable\",\"Q4 missing\"]"
        )
        answerStore.applyBatch(batch, listOf(RawAnswer(1, "A", "high", page = 1)), "REPLACE")

        val viewModel = AnswersViewModel(
            application = app,
            answerStore = answerStore,
            settingsRepo = settingsRepo,
            answerDao = db.answerDao()
        )

        // Wait for state collection
        val state = viewModel.uiState.first { it.answers.isNotEmpty() }
        assertEquals(2, state.warnings.size)
        assertTrue(state.warnings.contains("Photo 2 unreadable"))
        assertEquals("retake photo 2 and send all again", state.warningHint)
        assertFalse(state.isWarningsDismissed)

        // Dismiss warnings
        viewModel.dismissWarnings()
        val dismissedState = viewModel.uiState.first { it.isWarningsDismissed }
        assertTrue(dismissedState.isWarningsDismissed)
    }
}
