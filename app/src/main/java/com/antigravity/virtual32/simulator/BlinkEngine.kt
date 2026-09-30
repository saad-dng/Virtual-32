package com.antigravity.virtual32.simulator

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class LampsState(
    val blueOn: Boolean = false,
    val redOn: Boolean = false
)

enum class LedColor { BLUE, RED }

class BlinkEngine(private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(LampsState())
    val state: StateFlow<LampsState> = _state.asStateFlow()

    private var currentJob: Job? = null

    fun play(color: LedColor, pattern: List<BlinkStep>, loop: Boolean = false) {
        currentJob?.cancel()
        _state.value = LampsState() // Reset both LEDs

        currentJob = scope.launch {
            try {
                do {
                    for (step in pattern) {
                        if (color == LedColor.BLUE) {
                            _state.value = _state.value.copy(blueOn = step.on)
                        } else {
                            _state.value = _state.value.copy(redOn = step.on)
                        }
                        if (step.durationMs > 0) {
                            delay(step.durationMs)
                        }
                    }
                } while (loop && isActive)
                
                // Ensure turned off at the end
                if (color == LedColor.BLUE) {
                    _state.value = _state.value.copy(blueOn = false)
                } else {
                    _state.value = _state.value.copy(redOn = false)
                }
            } catch (e: CancellationException) {
                // Turn off if cancelled
                _state.value = LampsState()
                throw e
            }
        }
    }

    fun stop() {
        currentJob?.cancel()
        _state.value = LampsState()
    }
}
