package com.antigravity.virtual32.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.antigravity.virtual32.camera.JpegQualityPreset
import com.antigravity.virtual32.camera.Ov3660Resolution
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "anti_gravity_settings")

/**
 * Persists and retrieves application settings using Jetpack Preferences DataStore.
 * Supports both Phone 2 (Camera Twin) and Phone 1 (Receiver Brain) configuration.
 */
class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val SERVER_IP = stringPreferencesKey("server_ip")
        val SERVER_PORT = intPreferencesKey("server_port")
        val RESOLUTION_LABEL = stringPreferencesKey("resolution_label")
        val JPEG_QUALITY = intPreferencesKey("jpeg_quality")
        val APP_MODE = stringPreferencesKey("app_mode")
        val RECEIVER_PORT = intPreferencesKey("receiver_port")
        val GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val GEMINI_PROMPT = stringPreferencesKey("gemini_prompt")
        val HAS_COMPLETED_WELCOME = androidx.datastore.preferences.core.booleanPreferencesKey("has_completed_welcome")
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
            val ip = preferences[PreferencesKeys.SERVER_IP] ?: AppSettings.DEFAULT_SERVER_IP
            val port = preferences[PreferencesKeys.SERVER_PORT] ?: AppSettings.DEFAULT_SERVER_PORT
            val resLabel = preferences[PreferencesKeys.RESOLUTION_LABEL] ?: Ov3660Resolution.DEFAULT.name
            val resolution = Ov3660Resolution.fromLabel(resLabel)
            val quality = preferences[PreferencesKeys.JPEG_QUALITY] ?: JpegQualityPreset.DEFAULT.qualityPercentage
            val modeStr = preferences[PreferencesKeys.APP_MODE] ?: AppMode.CAMERA_TWIN.name
            val mode = runCatching { AppMode.valueOf(modeStr) }.getOrDefault(AppMode.CAMERA_TWIN)
            val recPort = preferences[PreferencesKeys.RECEIVER_PORT] ?: AppSettings.DEFAULT_RECEIVER_PORT
            val apiKey = preferences[PreferencesKeys.GEMINI_API_KEY] ?: ""
            val prompt = preferences[PreferencesKeys.GEMINI_PROMPT] ?: AppSettings.DEFAULT_GEMINI_PROMPT
            val completedWelcome = preferences[PreferencesKeys.HAS_COMPLETED_WELCOME] ?: false

            AppSettings(
                serverIp = ip,
                serverPort = port,
                resolution = resolution,
                jpegQuality = quality,
                appMode = mode,
                receiverPort = recPort,
                geminiApiKey = apiKey,
                geminiPrompt = prompt,
                hasCompletedWelcome = completedWelcome
            )
        }

    suspend fun updateSettings(
        serverIp: String,
        serverPort: Int,
        resolution: Ov3660Resolution,
        jpegQuality: Int = JpegQualityPreset.DEFAULT.qualityPercentage
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SERVER_IP] = serverIp.trim()
            preferences[PreferencesKeys.SERVER_PORT] = serverPort
            preferences[PreferencesKeys.RESOLUTION_LABEL] = resolution.name
            preferences[PreferencesKeys.JPEG_QUALITY] = jpegQuality.coerceIn(1, 100)
        }
    }

    suspend fun updateAppMode(mode: AppMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.APP_MODE] = mode.name
        }
    }

    suspend fun updateGeminiConfig(apiKey: String, prompt: String, receiverPort: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GEMINI_API_KEY] = apiKey.trim()
            preferences[PreferencesKeys.GEMINI_PROMPT] = prompt.trim()
            preferences[PreferencesKeys.RECEIVER_PORT] = receiverPort
        }
    }

    suspend fun setCompletedWelcome(completed: Boolean = true) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HAS_COMPLETED_WELCOME] = completed
        }
    }
}
