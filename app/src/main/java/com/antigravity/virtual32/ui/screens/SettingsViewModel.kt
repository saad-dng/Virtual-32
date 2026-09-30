package com.antigravity.virtual32.ui.screens

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.data.AppDatabase
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import com.antigravity.virtual32.settings.AiProvider
import java.io.InputStream
import java.io.OutputStream

data class UsageStats(
    val photosToday: Int = 0,
    val okCount: Int = 0,
    val unclearCount: Int = 0,
    val errorCount: Int = 0,
    val avgLatency: Long = 0L,
    val geminiCount: Int = 0,
    val claudeCount: Int = 0,
    val rateLimitCount: Int = 0
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val db = AppDatabase.getDatabase(application)
    
    val settings = settingsRepo.settingsFlow

    private val _usageStats = MutableStateFlow(UsageStats())
    val usageStats: StateFlow<UsageStats> = _usageStats.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            // Stats aggregation logic
            val startOfDay = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 0)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
                set(java.util.Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dao = db.answerDao()
            val photosToday = dao.getPhotosSince(startOfDay)
            val okCount = dao.getCountByStatus("ok")
            val unclearCount = dao.getCountByStatus("unclear")
            val errorCount = dao.getCountByStatus("error")
            val avgLatency = dao.getAverageLatency() ?: 0L
            val geminiCount = dao.getCountByProvider("GEMINI")
            val claudeCount = dao.getCountByProvider("CLAUDE")
            val rateLimitCount = dao.getRateLimitCount()
            
            _usageStats.value = UsageStats(
                photosToday = photosToday,
                okCount = okCount,
                unclearCount = unclearCount,
                errorCount = errorCount,
                avgLatency = avgLatency,
                geminiCount = geminiCount,
                claudeCount = claudeCount,
                rateLimitCount = rateLimitCount
            )
        }
    }

    fun setHapticsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateSettings { it.copy(enableHaptics = enabled) }
        }
    }

    fun exportSettings(outStream: OutputStream, includeKeys: Boolean) {
        viewModelScope.launch {
            val current = settingsRepo.getSettings()
            val backup = if (!includeKeys) current.copy(geminiKey = "", claudeKey = "") else current
            
            val json = buildJsonObject {
                put("provider", backup.provider.name)
                put("geminiKey", backup.geminiKey)
                put("geminiModel", backup.geminiModel)
                put("claudeKey", backup.claudeKey)
                put("claudeModel", backup.claudeModel)
                put("activePromptId", backup.activePromptId)
                put("answerMode", backup.answerMode.name)
                put("saveToGallery", backup.saveToGallery)
                put("enableHaptics", backup.enableHaptics)
            }
            
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                outStream.writer().use { it.write(json.toString()) }
            }
        }
    }

    fun importSettings(inputStream: InputStream) {
        viewModelScope.launch {
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                val jsonStr = inputStream.reader().readText()
                val json = kotlinx.serialization.json.Json.parseToJsonElement(jsonStr).jsonObject
                
                settingsRepo.updateSettings { current ->
                    current.copy(
                        provider = AiProvider.valueOf(json["provider"]?.jsonPrimitive?.content ?: current.provider.name),
                        geminiKey = json["geminiKey"]?.jsonPrimitive?.content.takeIf { !it.isNullOrBlank() } ?: current.geminiKey,
                        geminiModel = json["geminiModel"]?.jsonPrimitive?.content ?: current.geminiModel,
                        claudeKey = json["claudeKey"]?.jsonPrimitive?.content.takeIf { !it.isNullOrBlank() } ?: current.claudeKey,
                        claudeModel = json["claudeModel"]?.jsonPrimitive?.content ?: current.claudeModel,
                        activePromptId = json["activePromptId"]?.jsonPrimitive?.content ?: current.activePromptId,
                        answerMode = com.antigravity.virtual32.settings.AnswerMode.valueOf(json["answerMode"]?.jsonPrimitive?.content ?: current.answerMode.name),
                        saveToGallery = json["saveToGallery"]?.jsonPrimitive?.booleanOrNull ?: current.saveToGallery,
                        enableHaptics = json["enableHaptics"]?.jsonPrimitive?.booleanOrNull ?: current.enableHaptics
                    )
                }
            }
        }
    }
}
