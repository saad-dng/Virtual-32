package com.antigravity.virtual32.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

@Serializable
data class PromptPreset(
    val id: String,
    val name: String,
    val instruction: String,
    val isBuiltIn: Boolean = false
)

private val Context.promptDataStore: DataStore<Preferences> by preferencesDataStore(name = "prompts")

class PromptRepository(private val context: Context?) {

    private val PRESETS_KEY = stringPreferencesKey("custom_presets")

    val defaultPresets = listOf(
        PromptPreset(
            id = "default",
            name = "Standard MCQ (A-D)",
            instruction = "Read every multiple-choice question on the page. Use the printed question numbers. Choose the single best option from A, B, C, or D.",
            isBuiltIn = true
        ),
        PromptPreset(
            id = "mcq_e",
            name = "MCQ up to E",
            instruction = "Read every multiple-choice question on the page. Use the printed question numbers. Choose the single best option from A, B, C, D, or E.",
            isBuiltIn = true
        ),
        PromptPreset(
            id = "true_false",
            name = "True/False (T -> A, F -> B)",
            instruction = "Read every True/False question on the page. Use the printed question numbers. If the answer is True, output 'A'. If the answer is False, output 'B'. Do not output 'T' or 'F'.",
            isBuiltIn = true
        ),
        PromptPreset(
            id = "careful_columns",
            name = "Careful numbering / multi-column",
            instruction = "Carefully read the page. Pay attention to multi-column layouts and ensure you read questions in the correct numbered order. Choose the single best option for each question.",
            isBuiltIn = true
        )
    )

    val presetsFlow: Flow<List<PromptPreset>>
        get() = context!!.promptDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(androidx.datastore.preferences.core.emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                val customJson = preferences[PRESETS_KEY] ?: "[]"
                val customPresets = try {
                    Json.decodeFromString<List<PromptPreset>>(customJson)
                } catch (e: Exception) {
                    emptyList()
                }
                defaultPresets + customPresets
            }

    suspend fun saveCustomPreset(preset: PromptPreset) {
        if (preset.isBuiltIn) return
        context!!.promptDataStore.edit { preferences ->
            val customJson = preferences[PRESETS_KEY] ?: "[]"
            val customPresets = try {
                Json.decodeFromString<List<PromptPreset>>(customJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            val index = customPresets.indexOfFirst { it.id == preset.id }
            if (index >= 0) {
                customPresets[index] = preset
            } else {
                customPresets.add(preset)
            }
            
            preferences[PRESETS_KEY] = Json.encodeToString(customPresets)
        }
    }

    suspend fun deleteCustomPreset(id: String) {
        context!!.promptDataStore.edit { preferences ->
            val customJson = preferences[PRESETS_KEY] ?: "[]"
            val customPresets = try {
                Json.decodeFromString<List<PromptPreset>>(customJson).toMutableList()
            } catch (e: Exception) {
                mutableListOf()
            }
            
            customPresets.removeAll { it.id == id && !it.isBuiltIn }
            preferences[PRESETS_KEY] = Json.encodeToString(customPresets)
        }
    }
}
