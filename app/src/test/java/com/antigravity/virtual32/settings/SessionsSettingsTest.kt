package com.antigravity.virtual32.settings

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.antigravity.virtual32.ui.screens.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SessionsSettingsTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var app: Application
    private lateinit var repo: SettingsRepository
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext()
        repo = SettingsRepository(app)
        viewModel = SettingsViewModel(app, repo)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSessionsSettingsPersistence() = runBlocking {
        // 1. Max session pages
        viewModel.setMaxSessionPages(16)
        val s1 = repo.settingsFlow.first { it.maxSessionPages == 16 }
        assertEquals(16, s1.maxSessionPages)

        // 2. Auto-submit seconds
        viewModel.setSessionAutoSubmitSec(20)
        val s2 = repo.settingsFlow.first { it.sessionAutoSubmitSec == 20 }
        assertEquals(20, s2.sessionAutoSubmitSec)

        // 3. Downscale session payload toggle
        viewModel.setDownscaleSessionPayload(false)
        val s3 = repo.settingsFlow.first { !it.downscaleSessionPayload }
        assertFalse(s3.downscaleSessionPayload)

        // 4. Custom multi-photo instruction & reset
        val customText = "Custom multi photo prompt instructions"
        viewModel.setMultiPhotoInstruction(customText)
        val s4 = repo.settingsFlow.first { it.multiPhotoInstruction == customText }
        assertEquals(customText, s4.multiPhotoInstruction)

        // Reset to default
        viewModel.resetMultiPhotoInstruction()
        val s5 = repo.settingsFlow.first { it.multiPhotoInstruction != customText }
        assertEquals(AppSettings().multiPhotoInstruction, s5.multiPhotoInstruction)
    }
}
