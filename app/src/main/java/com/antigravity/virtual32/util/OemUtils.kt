package com.antigravity.virtual32.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object OemUtils {
    fun getOemGuidance(manufacturer: String): String? {
        val lower = manufacturer.lowercase()
        return when {
            lower.contains("xiaomi") || lower.contains("poco") || lower.contains("redmi") -> "Enable 'Autostart' and set Battery Saver to 'No Restrictions'."
            lower.contains("oppo") || lower.contains("realme") || lower.contains("oneplus") -> "Allow 'Auto launch' and 'Allow background activity' in App Info > Battery usage."
            lower.contains("vivo") || lower.contains("iqoo") -> "Enable 'High background power consumption' or 'Background autostart' in Battery settings."
            lower.contains("samsung") -> "Turn off 'Put app to sleep' in App Info > Battery, and allow background activity."
            lower.contains("huawei") || lower.contains("honor") -> "Set app launch to 'Manage manually' and enable all toggles (Auto-launch, Secondary launch, Run in background)."
            lower.contains("infinix") || lower.contains("tecno") -> "Allow auto-start in Phone Master or App Management."
            else -> null
        }
    }

    fun getOemIntents(manufacturer: String): List<Intent> {
        val lower = manufacturer.lowercase()
        return when {
            lower.contains("xiaomi") || lower.contains("poco") || lower.contains("redmi") -> listOf(
                Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            lower.contains("oppo") || lower.contains("realme") || lower.contains("oneplus") -> listOf(
                Intent().setClassName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                Intent().setClassName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            )
            lower.contains("vivo") || lower.contains("iqoo") -> listOf(
                Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                Intent().setClassName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
                Intent().setClassName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity")
            )
            lower.contains("samsung") -> listOf(
                Intent().setClassName("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity")
            )
            lower.contains("huawei") || lower.contains("honor") -> listOf(
                Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                Intent().setClassName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity")
            )
            else -> emptyList()
        }
    }
    
    fun openOemSettings(context: Context, manufacturer: String): Boolean {
        val intents = getOemIntents(manufacturer)
        var started = false
        for (intent in intents) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                started = true
                break
            } catch (e: Exception) {
                // Ignore and try next
            }
        }
        
        if (!started) {
            // Fallback
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                started = true
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return started
    }
}
