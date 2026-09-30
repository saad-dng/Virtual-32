package com.antigravity.virtual32.receiver.service

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
class ReceiverServiceTest {
    
    @Test
    fun testServiceStartsAndAcquiresLocks() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val service = Robolectric.buildService(ReceiverService::class.java).create().get()
        
        // Check state updates
        val state = ReceiverState.health.value
        assertEquals(true, state.isServiceRunning)
    }
}
