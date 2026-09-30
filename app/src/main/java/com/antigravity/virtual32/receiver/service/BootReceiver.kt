package com.antigravity.virtual32.receiver.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val settingsRepo = SettingsRepository(context)
            val startOnBoot = runBlocking { settingsRepo.getSettings().startOnBoot }
            if (startOnBoot) {
                val serviceIntent = Intent(context, ReceiverService::class.java)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(serviceIntent)
                }
            }
        }
    }
}
