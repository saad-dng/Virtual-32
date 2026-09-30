package com.antigravity.virtual32.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.virtual32.settings.SettingsRepository
import com.antigravity.virtual32.simulator.BlinkEngine
import com.antigravity.virtual32.simulator.BlinkPatterns
import com.antigravity.virtual32.simulator.DoubleTapDetector
import com.antigravity.virtual32.simulator.LampsState
import com.antigravity.virtual32.simulator.LedColor
import com.antigravity.virtual32.simulator.SimClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

class SimulatorViewModel(application: Application) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)
    private val blinkEngine = BlinkEngine(viewModelScope)
    
    private val _lamps = MutableStateFlow(LampsState())
    val lamps: StateFlow<LampsState> = _lamps.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val doubleTapDetector = DoubleTapDetector(
        scope = viewModelScope,
        onSingleTap = { handleNext() },
        onDoubleTap = { handleRepeat() }
    )

    private var simClient: SimClient? = null

    init {
        viewModelScope.launch {
            blinkEngine.state.collect { _lamps.value = it }
        }
        
        viewModelScope.launch {
            val settings = settingsRepo.getSettings()
            val host = if (settings.simLoopback) "127.0.0.1" else settings.simHost
            simClient = SimClient(host, settings.simPort)
            
            // Heartbeat
            while (true) {
                val up = simClient?.ping() ?: false
                _isConnected.value = up
                kotlinx.coroutines.delay(10000)
            }
        }
    }

    fun onButton1(jpeg: ByteArray) {
        viewModelScope.launch {
            blinkEngine.play(LedColor.BLUE, BlinkPatterns.processingPulse, loop = true)
            val res = simClient?.uploadWithRetries(jpeg)
            blinkEngine.stop()
            if (res == null) {
                blinkEngine.play(LedColor.RED, BlinkPatterns.serverUnreachable)
            } else {
                val status = res["status"]?.jsonPrimitive?.content
                if (status == "ok") {
                    blinkEngine.play(LedColor.BLUE, BlinkPatterns.readyUploadOk)
                } else if (status == "unclear") {
                    blinkEngine.play(LedColor.RED, BlinkPatterns.photoUnclear)
                } else {
                    blinkEngine.play(LedColor.RED, BlinkPatterns.serverError)
                }
            }
        }
    }

    fun onButton2() {
        doubleTapDetector.onTap()
    }

    private fun handleNext() {
        viewModelScope.launch {
            val res = simClient?.next()
            handleResult(res)
        }
    }

    private fun handleRepeat() {
        viewModelScope.launch {
            val res = simClient?.repeat()
            handleResult(res)
        }
    }

    private fun handleResult(res: kotlinx.serialization.json.JsonObject?) {
        if (res == null) {
            blinkEngine.play(LedColor.RED, BlinkPatterns.serverUnreachable)
            return
        }
        
        val ok = res["ok"]?.jsonPrimitive?.booleanOrNull ?: false
        if (!ok) {
            val reason = res["reason"]?.jsonPrimitive?.content
            if (reason == "empty") {
                blinkEngine.play(LedColor.RED, BlinkPatterns.noAnswersYet)
            } else {
                blinkEngine.play(LedColor.RED, BlinkPatterns.serverError)
            }
            return
        }
        
        val end = res["end"]?.jsonPrimitive?.booleanOrNull ?: false
        if (end) {
            blinkEngine.play(LedColor.RED, BlinkPatterns.cycleComplete)
            return
        }
        
        val blinks = res["blinks"]?.jsonPrimitive?.intOrNull
        if (blinks != null) {
            blinkEngine.play(LedColor.BLUE, BlinkPatterns.answerBlinks(blinks))
        }
    }

    fun updateSettings(host: String, port: Int, loopback: Boolean, res: String, qual: Int, saveGallery: Boolean) {
        viewModelScope.launch {
            settingsRepo.updateSettings { 
                it.copy(
                    simHost = host, 
                    simPort = port, 
                    simLoopback = loopback,
                    simResolution = res,
                    simJpegQuality = qual,
                    saveToGallery = saveGallery
                ) 
            }
            val h = if (loopback) "127.0.0.1" else host
            simClient = SimClient(h, port)
        }
    }

    fun testConnection() {
        viewModelScope.launch {
            val up = simClient?.ping() ?: false
            _isConnected.value = up
        }
    }
}
