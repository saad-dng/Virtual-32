package com.antigravity.virtual32

import android.app.Application
import android.os.StrictMode
import android.os.SystemClock
import android.util.Log

class Virtual32App : Application() {
    companion object {
        var appStartTimeMs: Long = 0L
    }

    override fun onCreate() {
        appStartTimeMs = SystemClock.uptimeMillis()
        super.onCreate()

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
            Log.d("Virtual32Startup", "StrictMode configured with penaltyLog")
        }
    }
}
