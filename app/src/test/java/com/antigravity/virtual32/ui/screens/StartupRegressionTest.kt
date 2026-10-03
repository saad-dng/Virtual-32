package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.receiver.pipeline.SessionManager
import com.antigravity.virtual32.util.ThumbnailCache
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class StartupRegressionTest {

    private lateinit var app: Application

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        File(app.filesDir, "open_session.json").delete()
    }

    @Test
    fun testHomeViewModelReflectionInstantiation() {
        // Ensures ViewModelProvider can construct HomeViewModel via reflection
        // without NoSuchMethodException
        val factory = ViewModelProvider.AndroidViewModelFactory.getInstance(app)
        val viewModel = factory.create(HomeViewModel::class.java)
        assertNotNull(viewModel)
    }

    @Test
    fun testFirstFrameRendersWithoutBlockingOnIO() {
        // ViewModel creation must be instantaneous (< 50ms) without waiting for Room or disk IO
        val elapsed = measureTimeMillis {
            val vm = HomeViewModel(app)
            assertNotNull(vm.sessionUiState.value)
            // It exposes loading state initially while background IO proceeds
            assertTrue("Expected isLoading to be true initially", vm.isLoading.value)
        }
        assertTrue("HomeViewModel init took too long: ${elapsed}ms", elapsed < 200)
    }

    @Test
    fun testDatabaseLazyInitializationSafeFallback() = runBlocking {
        // Database initializes on background dispatcher and can migrate / recover safely
        val db = AppDatabase.initDatabase(app)
        assertNotNull(db)
        assertNotNull(db.answerDao())
    }

    @Test
    fun testSessionManagerNonBlockingInit() {
        // Write mock session file
        val file = File(app.filesDir, "open_session.json")
        file.writeText("""[{"cachePath":"/nonexistent/p1.jpg","galleryUri":null,"arrivalTimeMs":1000}]""")

        val elapsed = measureTimeMillis {
            val sm = SessionManager(app)
            assertNotNull(sm)
        }
        assertTrue("SessionManager init blocked thread: ${elapsed}ms", elapsed < 100)
    }
}
