package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.settings.AiProvider
import com.antigravity.virtual32.settings.AppSettings
import com.antigravity.virtual32.settings.PromptPreset
import com.antigravity.virtual32.settings.PromptRepository
import com.antigravity.virtual32.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import com.antigravity.virtual32.receiver.ai.ClaudeProvider
import com.antigravity.virtual32.receiver.ai.GeminiProvider
import com.antigravity.virtual32.receiver.ai.PromptBuilder
import java.util.UUID

data class AiSettingsUiState(
    val settings: AppSettings = AppSettings(),
    val presets: List<PromptPreset> = emptyList(),
    val activePreset: PromptPreset? = null,
    val editedInstruction: String = "",
    val hasUnsavedChanges: Boolean = false,
    val testKeyResult: String? = null,
    val testPromptResult: String? = null,
    val isTesting: Boolean = false
)

class AiSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val promptRepo = PromptRepository(application)
    private val okHttpClient = OkHttpClient()

    private val _uiState = MutableStateFlow(AiSettingsUiState())
    val uiState: StateFlow<AiSettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepo.settingsFlow.collect { settings ->
                val presets = promptRepo.presetsFlow.first()
                val activePreset = presets.find { it.id == settings.activePromptId } ?: promptRepo.defaultPresets.first()
                
                // Only update editedInstruction if we haven't modified it manually
                val currentEdited = _uiState.value.editedInstruction
                val newEdited = if (_uiState.value.hasUnsavedChanges) currentEdited else activePreset.instruction
                
                _uiState.value = _uiState.value.copy(
                    settings = settings,
                    presets = presets,
                    activePreset = activePreset,
                    editedInstruction = newEdited,
                    hasUnsavedChanges = _uiState.value.hasUnsavedChanges
                )
            }
        }
        viewModelScope.launch {
            promptRepo.presetsFlow.collect { presets ->
                val settings = settingsRepo.getSettings()
                val activePreset = presets.find { it.id == settings.activePromptId } ?: promptRepo.defaultPresets.first()
                val currentEdited = _uiState.value.editedInstruction
                val newEdited = if (_uiState.value.hasUnsavedChanges) currentEdited else activePreset.instruction
                
                _uiState.value = _uiState.value.copy(
                    presets = presets,
                    activePreset = activePreset,
                    editedInstruction = newEdited
                )
            }
        }
    }

    fun updateSettings(updater: (AppSettings) -> AppSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(updater)
        }
    }

    fun setInstruction(text: String) {
        val active = _uiState.value.activePreset
        val hasChanges = active?.instruction != text
        _uiState.value = _uiState.value.copy(
            editedInstruction = text,
            hasUnsavedChanges = hasChanges
        )
    }

    fun savePreset(name: String) {
        viewModelScope.launch {
            val active = _uiState.value.activePreset
            if (active != null && !active.isBuiltIn && active.name == name) {
                // Update existing
                val updated = active.copy(instruction = _uiState.value.editedInstruction)
                promptRepo.saveCustomPreset(updated)
                _uiState.value = _uiState.value.copy(hasUnsavedChanges = false)
            } else {
                // Save as new
                val newId = UUID.randomUUID().toString()
                val newPreset = PromptPreset(
                    id = newId,
                    name = name,
                    instruction = _uiState.value.editedInstruction,
                    isBuiltIn = false
                )
                promptRepo.saveCustomPreset(newPreset)
                updateSettings { it.copy(activePromptId = newId) }
                _uiState.value = _uiState.value.copy(hasUnsavedChanges = false)
            }
        }
    }

    fun setActivePreset(id: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(hasUnsavedChanges = false)
            updateSettings { it.copy(activePromptId = id) }
        }
    }

    fun deletePreset(id: String) {
        viewModelScope.launch {
            promptRepo.deleteCustomPreset(id)
            val settings = settingsRepo.getSettings()
            if (settings.activePromptId == id) {
                updateSettings { it.copy(activePromptId = "default") }
            }
        }
    }

    fun resetToDefault() {
        viewModelScope.launch {
            val active = _uiState.value.activePreset
            if (active != null) {
                val defaultContent = promptRepo.defaultPresets.find { it.id == active.id }?.instruction ?: promptRepo.defaultPresets.first().instruction
                _uiState.value = _uiState.value.copy(
                    editedInstruction = defaultContent,
                    hasUnsavedChanges = defaultContent != active.instruction
                )
            }
        }
    }

    fun testKey() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTesting = true, testKeyResult = "Testing...")
            val settings = settingsRepo.getSettings()
            val provider = if (settings.provider == AiProvider.GEMINI) {
                GeminiProvider(okHttpClient, settings.geminiKey)
            } else {
                ClaudeProvider(okHttpClient, settings.claudeKey, settings.claudeModel)
            }
            
            // 1x1 black JPEG base64
            val tinyJpegBase64 = "ffd8ffe000104a46494600010101004800480000ffdb0043000101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010101010100ffc0000b080001000101011100ffc4001f0000010501010101010100000000000000000102030405060708090a0bffda0008010100003f003f00ffd9"
            val tinyJpeg = tinyJpegBase64.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

            try {
                val res = provider.analyze(tinyJpeg, "Test connection")
                _uiState.value = _uiState.value.copy(isTesting = false, testKeyResult = "OK (Status: ${res.status})")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isTesting = false, testKeyResult = "Error: ${e.message}")
            }
        }
    }

    fun testPrompt(imageBytes: ByteArray?) {
        if (imageBytes == null) {
            _uiState.value = _uiState.value.copy(testPromptResult = "No image selected")
            return
        }
        viewModelScope.launch {
            val startTime = System.currentTimeMillis()
            _uiState.value = _uiState.value.copy(isTesting = true, testPromptResult = "Running AI...")
            val settings = settingsRepo.getSettings()
            val provider = if (settings.provider == AiProvider.GEMINI) {
                GeminiProvider(okHttpClient, settings.geminiKey)
            } else {
                ClaudeProvider(okHttpClient, settings.claudeKey, settings.claudeModel)
            }
            
            val instruction = PromptBuilder.build(_uiState.value.editedInstruction, settings.includeReasoning)
            try {
                val res = provider.analyze(imageBytes, instruction)
                val latency = System.currentTimeMillis() - startTime
                _uiState.value = _uiState.value.copy(
                    isTesting = false, 
                    testPromptResult = "Latency: ${latency}ms\nStatus: ${res.status}\nAnswers: ${res.answers.size}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isTesting = false, testPromptResult = "Error: ${e.message}")
            }
        }
    }
}
