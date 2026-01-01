package org.menagerie.puppet_master

import androidx.compose.ui.input.key.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random

class PuppetStateController(
    private val scope: CoroutineScope,
    private val dataManager: PuppetDataManager,
    private val audioProcessor: AudioProcessor,
    private val getUiState: () -> UiState,
    private val thresholds: StateFlow<Map<Float, PuppetStateInfo?>>
) {
    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    val activeState: StateFlow<PuppetStateInfo?> = _activeState.asStateFlow()

    private val _displayedImageName = MutableStateFlow<String?>(null)
    val displayedImageName: StateFlow<String?> = _displayedImageName.asStateFlow()

    private val _activeSpecialEffect = MutableStateFlow<ActiveSpecialEffect?>(null)
    val activeSpecialEffect: StateFlow<ActiveSpecialEffect?> = _activeSpecialEffect.asStateFlow()

    private val _isBlinking = MutableStateFlow(false)
    val isBlinking: StateFlow<Boolean> = _isBlinking.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _rawAudioLevel = MutableStateFlow(0f)
    val rawAudioLevel: StateFlow<Float> = _rawAudioLevel.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _frequencyData = MutableStateFlow(FloatArray(0))
    val frequencyData: StateFlow<FloatArray> = _frequencyData.asStateFlow()

    private var clientBlinkingJob: Job? = null
    private var returnToIdleJob: Job? = null
    private var hotkeyStateActive = false

    var operatingMode: OperatingMode = OperatingMode.OFFLINE
    var controlMode: ControlMode = ControlMode.DIRECT
    var isPublishing: Boolean = false

    private var smoothedLevel: Float = 0f

    init {
        scope.launch {
            dataManager.activePuppet.collect { puppet ->
                val currentActiveStateName = _activeState.value?.name
                _activeState.value = puppet?.states?.find { it.name == currentActiveStateName } ?: puppet?.states?.find { it.name == "idle" }
                _audioLevel.value = 0f
                _rawAudioLevel.value = 0f
            }
        }

        scope.launch {
            activeState.collect { state ->
                _displayedImageName.value = state?.imageName
                val effect = state?.appliedEffect
                if (effect != null) {
                    val newActiveEffect = if (getUiState().preserveState && activeSpecialEffect.value != null) {
                        activeSpecialEffect.value!!.copyWithPreservedStartTime(effect)
                    } else {
                        ActiveSpecialEffect(effect)
                    }
                    _activeSpecialEffect.value = newActiveEffect
                } else {
                    _activeSpecialEffect.value = null
                }
            }
        }
        startBlinkingLoop()
    }

    private fun startBlinkingLoop() {
        clientBlinkingJob = scope.launch {
            while (true) {
                val state = activeState.value
                val delayTime = if (state != null) {
                    if (state.minBlinkRate >= state.maxBlinkRate) {
                        state.maxBlinkRate
                    } else {
                        Random.nextLong(state.minBlinkRate, state.maxBlinkRate)
                    }
                } else {
                    5000L
                }
                delay(delayTime)

                val currentState = activeState.value ?: continue

                val shouldBlink = currentState.blinkImageName != null || currentState.eyeState?.eyes?.left?.closedState != null
                if (!shouldBlink) continue

                val isClientInControl = operatingMode == OperatingMode.OFFLINE || isPublishing

                if (isClientInControl) {
                    _isBlinking.value = true
                    if (currentState.blinkImageName != null) {
                        _displayedImageName.value = currentState.blinkImageName
                    }
                    delay(150)
                    _isBlinking.value = false
                    _displayedImageName.value = activeState.value?.imageName
                }
            }
        }
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        if (controlMode == ControlMode.DIRECT) {
            val puppet = dataManager.activePuppet.value ?: return
            for (state in puppet.states) {
                state.hotkey?.let { hotkey ->
                    if (hotkey.isHotkey(keyEvent)) {
                        if (hotkey.hold) {
                            if (hotkey.isDown) {
                                _activeState.value = state
                                hotkeyStateActive = true
                            } else {
                                hotkeyStateActive = false
                                returnToIdle()
                            }
                        } else {
                            if (_activeState.value == state && hotkeyStateActive) {
                                hotkeyStateActive = false
                                returnToIdle()
                            } else {
                                _activeState.value = state
                                hotkeyStateActive = true
                            }
                        }
                        return
                    }
                }
            }
        }
    }

    fun toggleListening() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    private fun startListening() {
        _isListening.value = true
        audioProcessor.start(
            onLevelChange = { rawLevel ->
                _rawAudioLevel.value = rawLevel

                val amplifiedLevel = (rawLevel * SENSITIVITY).coerceIn(0f, 1f)
                smoothedLevel += (amplifiedLevel - smoothedLevel) * SMOOTHING_FACTOR
                _audioLevel.value = smoothedLevel

                if (hotkeyStateActive) return@start

                val isControlling = operatingMode == OperatingMode.OFFLINE || isPublishing

                if (isControlling && controlMode == ControlMode.DIRECT) {
                    val scaledLevel = rawLevel.pow(0.5f)
                    val sortedThresholds = thresholds.value.entries.sortedBy { it.key }
                    val activeThresholdIndex = sortedThresholds.indexOfLast { scaledLevel >= it.key }

                    if (activeThresholdIndex != -1) {
                        returnToIdleJob?.cancel()
                        var state: PuppetStateInfo? = null
                        for (i in activeThresholdIndex downTo 0) {
                            if (sortedThresholds[i].value != null) {
                                state = sortedThresholds[i].value
                                break
                            }
                        }
                        if (state != null) {
                            _activeState.value = state
                        } else {
                            returnToIdle()
                        }
                    } else {
                        returnToIdle()
                    }
                }
            },
            onFrequencyData = {
                _frequencyData.value = it
            }
        )
    }

    private fun stopListening() {
        _isListening.value = false
        _audioLevel.value = 0f
        _rawAudioLevel.value = 0f
        _frequencyData.value = FloatArray(0)
        audioProcessor.stop()
    }

    fun setStateByName(stateName: String) {
        val puppet = dataManager.activePuppet.value ?: return
        val stateToSet = puppet.states.find { it.name == stateName }
        if (stateToSet != null) {
            _activeState.value = stateToSet
        }
    }

    fun setStateByNameWithEffect(stateName: String, effect: SpecialEffect?) {
        val puppet = dataManager.activePuppet.value ?: return
        val stateToSet = puppet.states.find { it.name == stateName }
        if (stateToSet != null) {
            _activeState.value = stateToSet.copy(appliedEffect = effect)
        }
    }

    private fun returnToIdle() {
        if (_activeState.value?.name != "idle") {
            returnToIdleJob?.cancel()
            returnToIdleJob = scope.launch {
                delay(100)
                _activeState.value = dataManager.activePuppet.value?.states?.find { it.name == "idle" }
            }
        }
    }

    fun setServerImage(imageName: String) {
        _displayedImageName.value = imageName
        val newActiveState = dataManager.activePuppet.value?.states?.find { it.imageName == imageName || it.blinkImageName == imageName }
        if (newActiveState != null && _activeState.value != newActiveState) {
            _activeState.value = newActiveState
        }
    }

    fun onOffline() {
        if (dataManager.activePuppet.value == null) {
            scope.launch { dataManager.reloadLastTroupe() }
        } else {
            _activeState.value = dataManager.activePuppet.value?.states?.find { it.name == "idle" }
        }
    }

    fun stopBlinking() {
        clientBlinkingJob?.cancel()
    }

    companion object {
        private const val SENSITIVITY = 10f
        private const val SMOOTHING_FACTOR = 0.1f
    }
}
