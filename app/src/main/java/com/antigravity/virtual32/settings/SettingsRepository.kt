package com.antigravity.virtual32.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

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
        val PAUSE_AI = booleanPreferencesKey("pause_ai")
        val REQUEST_TIMEOUT_SEC = intPreferencesKey("request_timeout_sec")
        val SIM_HOST = stringPreferencesKey("sim_host")
        val SIM_PORT = intPreferencesKey("sim_port")
        val SIM_LOOPBACK = booleanPreferencesKey("sim_loopback")
        val SIM_RESOLUTION = stringPreferencesKey("sim_resolution")
        val SIM_JPEG_QUALITY = intPreferencesKey("sim_jpeg_quality")
    }

    val settingsFlow: Flow<AppSettings> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            AppSettings(
                serverPort = preferences[PreferencesKeys.SERVER_PORT] ?: 5000,
                provider = try { AiProvider.valueOf(preferences[PreferencesKeys.PROVIDER] ?: "GEMINI") } catch (e: Exception) { AiProvider.GEMINI },
                fallbackProvider = try { AiProvider.valueOf(preferences[PreferencesKeys.FALLBACK_PROVIDER] ?: "NONE") } catch (e: Exception) { AiProvider.NONE },
                geminiModel = preferences[PreferencesKeys.GEMINI_MODEL] ?: "gemini-1.5-flash",
                claudeModel = preferences[PreferencesKeys.CLAUDE_MODEL] ?: "claude-sonnet-5-5",
                geminiKey = preferences[PreferencesKeys.GEMINI_KEY] ?: "",
                claudeKey = preferences[PreferencesKeys.CLAUDE_KEY] ?: "",
                activePromptId = preferences[PreferencesKeys.ACTIVE_PROMPT_ID] ?: "default",
                answerMode = try { AnswerMode.valueOf(preferences[PreferencesKeys.ANSWER_MODE] ?: "REPLACE") } catch (e: Exception) { AnswerMode.REPLACE },
                saveToGallery = preferences[PreferencesKeys.SAVE_TO_GALLERY] ?: true,
                pauseAi = preferences[PreferencesKeys.PAUSE_AI] ?: false,
                requestTimeoutSec = preferences[PreferencesKeys.REQUEST_TIMEOUT_SEC] ?: 40,
                simHost = preferences[PreferencesKeys.SIM_HOST] ?: "127.0.0.1",
                simPort = preferences[PreferencesKeys.SIM_PORT] ?: 5000,
                simLoopback = preferences[PreferencesKeys.SIM_LOOPBACK] ?: false,
                simResolution = preferences[PreferencesKeys.SIM_RESOLUTION] ?: "UXGA",
                simJpegQuality = preferences[PreferencesKeys.SIM_JPEG_QUALITY] ?: 80
            )
        }

    suspend fun updateSettings(update: (AppSettings) -> AppSettings) {
        context.dataStore.edit { preferences ->
            val current = AppSettings(
                serverPort = preferences[PreferencesKeys.SERVER_PORT] ?: 5000,
                provider = try { AiProvider.valueOf(preferences[PreferencesKeys.PROVIDER] ?: "GEMINI") } catch (e: Exception) { AiProvider.GEMINI },
                fallbackProvider = try { AiProvider.valueOf(preferences[PreferencesKeys.FALLBACK_PROVIDER] ?: "NONE") } catch (e: Exception) { AiProvider.NONE },
                geminiModel = preferences[PreferencesKeys.GEMINI_MODEL] ?: "gemini-1.5-flash",
                claudeModel = preferences[PreferencesKeys.CLAUDE_MODEL] ?: "claude-sonnet-5-5",
                geminiKey = preferences[PreferencesKeys.GEMINI_KEY] ?: "",
                claudeKey = preferences[PreferencesKeys.CLAUDE_KEY] ?: "",
                activePromptId = preferences[PreferencesKeys.ACTIVE_PROMPT_ID] ?: "default",
                answerMode = try { AnswerMode.valueOf(preferences[PreferencesKeys.ANSWER_MODE] ?: "REPLACE") } catch (e: Exception) { AnswerMode.REPLACE },
                saveToGallery = preferences[PreferencesKeys.SAVE_TO_GALLERY] ?: true,
                pauseAi = preferences[PreferencesKeys.PAUSE_AI] ?: false,
                requestTimeoutSec = preferences[PreferencesKeys.REQUEST_TIMEOUT_SEC] ?: 40,
                simHost = preferences[PreferencesKeys.SIM_HOST] ?: "127.0.0.1",
                simPort = preferences[PreferencesKeys.SIM_PORT] ?: 5000,
                simLoopback = preferences[PreferencesKeys.SIM_LOOPBACK] ?: false,
                simResolution = preferences[PreferencesKeys.SIM_RESOLUTION] ?: "UXGA",
                simJpegQuality = preferences[PreferencesKeys.SIM_JPEG_QUALITY] ?: 80
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
            preferences[PreferencesKeys.PAUSE_AI] = updated.pauseAi
            preferences[PreferencesKeys.REQUEST_TIMEOUT_SEC] = updated.requestTimeoutSec
            preferences[PreferencesKeys.SIM_HOST] = updated.simHost
            preferences[PreferencesKeys.SIM_PORT] = updated.simPort
            preferences[PreferencesKeys.SIM_LOOPBACK] = updated.simLoopback
            preferences[PreferencesKeys.SIM_RESOLUTION] = updated.simResolution
            preferences[PreferencesKeys.SIM_JPEG_QUALITY] = updated.simJpegQuality
        }
    }
}
