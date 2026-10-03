package com.antigravity.virtual32.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

open class SettingsRepository(private val context: Context?) {

    private object PreferencesKeys {
        val SERVER_PORT = intPreferencesKey("server_port")
        val PROVIDER = stringPreferencesKey("provider")
        val FALLBACK_PROVIDER = stringPreferencesKey("fallback_provider")
        val GEMINI_MODEL = stringPreferencesKey("gemini_model")
        val CLAUDE_MODEL = stringPreferencesKey("claude_model")
        val GEMINI_KEY = stringPreferencesKey("gemini_key")
        val CLAUDE_KEY = stringPreferencesKey("claude_key")
        val ACTIVE_PROMPT_ID = stringPreferencesKey("active_prompt_id")
        val ANSWER_MODE = stringPreferencesKey("answer_mode")
        val SAVE_TO_GALLERY = booleanPreferencesKey("save_to_gallery")
        val CONFIDENCE_FLAGS = booleanPreferencesKey("confidence_flags")
        val INCLUDE_REASONING = booleanPreferencesKey("include_reasoning")
        val PAUSE_AI = booleanPreferencesKey("pause_ai")
        val REQUEST_TIMEOUT_SEC = intPreferencesKey("request_timeout_sec")
        val SIM_HOST = stringPreferencesKey("sim_host")
        val SIM_PORT = intPreferencesKey("sim_port")
        val SIM_LOOPBACK = booleanPreferencesKey("sim_loopback")
        val SIM_RESOLUTION = stringPreferencesKey("sim_resolution")
        val SIM_JPEG_QUALITY = intPreferencesKey("sim_jpeg_quality")
        val START_ON_BOOT = booleanPreferencesKey("start_on_boot")
        val ENABLE_HAPTICS = booleanPreferencesKey("enable_haptics")
        val MAX_SESSION_PAGES = intPreferencesKey("max_session_pages")
        val SESSION_AUTO_SUBMIT_SEC = intPreferencesKey("session_auto_submit_sec")
        val MULTI_PHOTO_INSTRUCTION = stringPreferencesKey("multi_photo_instruction")
        val DOWNSCALE_SESSION_PAYLOAD = booleanPreferencesKey("downscale_session_payload")
    }

    open val settingsFlow: Flow<AppSettings>
        get() = context!!.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val defaultSettings = AppSettings()
            AppSettings(
                serverPort = preferences[PreferencesKeys.SERVER_PORT] ?: 5000,
                provider = try { AiProvider.valueOf(preferences[PreferencesKeys.PROVIDER] ?: "GEMINI") } catch (e: Exception) { AiProvider.GEMINI },
                fallbackProvider = try { AiProvider.valueOf(preferences[PreferencesKeys.FALLBACK_PROVIDER] ?: "NONE") } catch (e: Exception) { AiProvider.NONE },
                geminiModel = preferences[PreferencesKeys.GEMINI_MODEL]?.let { if (it == "gemini-1.5-flash" || it == "gemini-2.0-flash") "gemini-3.8-flash" else it } ?: "gemini-3.8-flash",
                claudeModel = preferences[PreferencesKeys.CLAUDE_MODEL]?.let { if (it == "claude-sonnet-5-5") "claude-3-5-sonnet-20241022" else it } ?: "claude-3-5-sonnet-20241022",
                geminiKey = preferences[PreferencesKeys.GEMINI_KEY] ?: "",
                claudeKey = preferences[PreferencesKeys.CLAUDE_KEY] ?: "",
                activePromptId = preferences[PreferencesKeys.ACTIVE_PROMPT_ID] ?: "default",
                answerMode = try { AnswerMode.valueOf(preferences[PreferencesKeys.ANSWER_MODE] ?: "REPLACE") } catch (e: Exception) { AnswerMode.REPLACE },
                saveToGallery = preferences[PreferencesKeys.SAVE_TO_GALLERY] ?: true,
                confidenceFlags = preferences[PreferencesKeys.CONFIDENCE_FLAGS] ?: true,
                includeReasoning = preferences[PreferencesKeys.INCLUDE_REASONING] ?: false,
                pauseAi = preferences[PreferencesKeys.PAUSE_AI] ?: false,
                requestTimeoutSec = preferences[PreferencesKeys.REQUEST_TIMEOUT_SEC] ?: 40,
                simHost = preferences[PreferencesKeys.SIM_HOST] ?: "127.0.0.1",
                simPort = preferences[PreferencesKeys.SIM_PORT] ?: 5000,
                simLoopback = preferences[PreferencesKeys.SIM_LOOPBACK] ?: false,
                simResolution = preferences[PreferencesKeys.SIM_RESOLUTION] ?: "UXGA",
                simJpegQuality = preferences[PreferencesKeys.SIM_JPEG_QUALITY] ?: 80,
                startOnBoot = preferences[PreferencesKeys.START_ON_BOOT] ?: false,
                enableHaptics = preferences[PreferencesKeys.ENABLE_HAPTICS] ?: false,
                maxSessionPages = preferences[PreferencesKeys.MAX_SESSION_PAGES] ?: 12,
                sessionAutoSubmitSec = preferences[PreferencesKeys.SESSION_AUTO_SUBMIT_SEC] ?: 0,
                multiPhotoInstruction = preferences[PreferencesKeys.MULTI_PHOTO_INSTRUCTION] ?: defaultSettings.multiPhotoInstruction,
                downscaleSessionPayload = preferences[PreferencesKeys.DOWNSCALE_SESSION_PAYLOAD] ?: true
            )
        }

    open suspend fun getSettings(): AppSettings = settingsFlow.first()

    open suspend fun updateSettings(update: (AppSettings) -> AppSettings) {
        context!!.dataStore.edit { preferences ->
            val defaultSettings = AppSettings()
            val current = AppSettings(
                serverPort = preferences[PreferencesKeys.SERVER_PORT] ?: 5000,
                provider = try { AiProvider.valueOf(preferences[PreferencesKeys.PROVIDER] ?: "GEMINI") } catch (e: Exception) { AiProvider.GEMINI },
                fallbackProvider = try { AiProvider.valueOf(preferences[PreferencesKeys.FALLBACK_PROVIDER] ?: "NONE") } catch (e: Exception) { AiProvider.NONE },
                geminiModel = preferences[PreferencesKeys.GEMINI_MODEL]?.let { if (it == "gemini-1.5-flash" || it == "gemini-2.0-flash") "gemini-3.8-flash" else it } ?: "gemini-3.8-flash",
                claudeModel = preferences[PreferencesKeys.CLAUDE_MODEL]?.let { if (it == "claude-sonnet-5-5") "claude-3-5-sonnet-20241022" else it } ?: "claude-3-5-sonnet-20241022",
                geminiKey = preferences[PreferencesKeys.GEMINI_KEY] ?: "",
                claudeKey = preferences[PreferencesKeys.CLAUDE_KEY] ?: "",
                activePromptId = preferences[PreferencesKeys.ACTIVE_PROMPT_ID] ?: "default",
                answerMode = try { AnswerMode.valueOf(preferences[PreferencesKeys.ANSWER_MODE] ?: "REPLACE") } catch (e: Exception) { AnswerMode.REPLACE },
                saveToGallery = preferences[PreferencesKeys.SAVE_TO_GALLERY] ?: true,
                confidenceFlags = preferences[PreferencesKeys.CONFIDENCE_FLAGS] ?: true,
                includeReasoning = preferences[PreferencesKeys.INCLUDE_REASONING] ?: false,
                pauseAi = preferences[PreferencesKeys.PAUSE_AI] ?: false,
                requestTimeoutSec = preferences[PreferencesKeys.REQUEST_TIMEOUT_SEC] ?: 40,
                simHost = preferences[PreferencesKeys.SIM_HOST] ?: "127.0.0.1",
                simPort = preferences[PreferencesKeys.SIM_PORT] ?: 5000,
                simLoopback = preferences[PreferencesKeys.SIM_LOOPBACK] ?: false,
                simResolution = preferences[PreferencesKeys.SIM_RESOLUTION] ?: "UXGA",
                simJpegQuality = preferences[PreferencesKeys.SIM_JPEG_QUALITY] ?: 80,
                startOnBoot = preferences[PreferencesKeys.START_ON_BOOT] ?: false,
                enableHaptics = preferences[PreferencesKeys.ENABLE_HAPTICS] ?: false,
                maxSessionPages = preferences[PreferencesKeys.MAX_SESSION_PAGES] ?: 12,
                sessionAutoSubmitSec = preferences[PreferencesKeys.SESSION_AUTO_SUBMIT_SEC] ?: 0,
                multiPhotoInstruction = preferences[PreferencesKeys.MULTI_PHOTO_INSTRUCTION] ?: defaultSettings.multiPhotoInstruction,
                downscaleSessionPayload = preferences[PreferencesKeys.DOWNSCALE_SESSION_PAYLOAD] ?: true
            )
            
            val updated = update(current)
            preferences[PreferencesKeys.SERVER_PORT] = updated.serverPort
            preferences[PreferencesKeys.PROVIDER] = updated.provider.name
            preferences[PreferencesKeys.FALLBACK_PROVIDER] = updated.fallbackProvider.name
            preferences[PreferencesKeys.GEMINI_MODEL] = updated.geminiModel
            preferences[PreferencesKeys.CLAUDE_MODEL] = updated.claudeModel
            preferences[PreferencesKeys.GEMINI_KEY] = updated.geminiKey
            preferences[PreferencesKeys.CLAUDE_KEY] = updated.claudeKey
            preferences[PreferencesKeys.ACTIVE_PROMPT_ID] = updated.activePromptId
            preferences[PreferencesKeys.ANSWER_MODE] = updated.answerMode.name
            preferences[PreferencesKeys.SAVE_TO_GALLERY] = updated.saveToGallery
            preferences[PreferencesKeys.CONFIDENCE_FLAGS] = updated.confidenceFlags
            preferences[PreferencesKeys.INCLUDE_REASONING] = updated.includeReasoning
            preferences[PreferencesKeys.PAUSE_AI] = updated.pauseAi
            preferences[PreferencesKeys.REQUEST_TIMEOUT_SEC] = updated.requestTimeoutSec
            preferences[PreferencesKeys.SIM_HOST] = updated.simHost
            preferences[PreferencesKeys.SIM_PORT] = updated.simPort
            preferences[PreferencesKeys.SIM_LOOPBACK] = updated.simLoopback
            preferences[PreferencesKeys.SIM_RESOLUTION] = updated.simResolution
            preferences[PreferencesKeys.SIM_JPEG_QUALITY] = updated.simJpegQuality
            preferences[PreferencesKeys.START_ON_BOOT] = updated.startOnBoot
            preferences[PreferencesKeys.ENABLE_HAPTICS] = updated.enableHaptics
            preferences[PreferencesKeys.MAX_SESSION_PAGES] = updated.maxSessionPages
            preferences[PreferencesKeys.SESSION_AUTO_SUBMIT_SEC] = updated.sessionAutoSubmitSec
            preferences[PreferencesKeys.MULTI_PHOTO_INSTRUCTION] = updated.multiPhotoInstruction
            preferences[PreferencesKeys.DOWNSCALE_SESSION_PAYLOAD] = updated.downscaleSessionPayload
        }
    }
}
