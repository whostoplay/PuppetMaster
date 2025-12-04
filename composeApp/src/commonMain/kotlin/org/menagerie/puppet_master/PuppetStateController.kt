package org.menagerie.puppet_master

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

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private var clientBlinkingJob: Job? = null
    private var returnToIdleJob: Job? = null

    var operatingMode: OperatingMode = OperatingMode.OFFLINE
    var isPublishing: Boolean = false

    init {
        scope.launch {
            dataManager.activePuppet.collect { puppet ->
                val currentActiveStateName = _activeState.value?.name
                _activeState.value = puppet?.states?.find { it.name == currentActiveStateName } ?: puppet?.states?.find { it.name == "idle" }
                _audioLevel.value = 0f
            }
        }

        scope.launch {
            activeState.collect { state ->
                clientBlinkingJob?.cancel()
                _displayedImageName.value = state?.imageName
                val effect = state?.appliedEffect
                 println(state)
                if (effect != null) {
                    val newEffect = ActiveSpecialEffect(effect)
                    if (getUiState().preserveState) {
                        newEffect.preserveStartTime(activeSpecialEffect.value)
                    }
                    _activeSpecialEffect.value = newEffect
                } else {
                    _activeSpecialEffect.value = null
                }

                if (state != null && (state.blinkImageName != null || state.eyeState?.eyes?.left?.closedState != null)) {
                    clientBlinkingJob = scope.launch {
                        while (true) {
                            val delayTime = if (state.minBlinkRate >= state.maxBlinkRate) {
                                state.maxBlinkRate
                            } else {
                                Random.nextLong(state.minBlinkRate, state.maxBlinkRate)
                            }
                            delay(delayTime)

                            val isClientInControl = operatingMode == OperatingMode.OFFLINE || isPublishing

                            if (isClientInControl && activeState.value == state) {
                                _isBlinking.value = true
                                if (state.blinkImageName != null) {
                                    _displayedImageName.value = state.blinkImageName
                                }
                                delay(150)
                                _isBlinking.value = false
                                if (state.blinkImageName != null) {
                                    _displayedImageName.value = state.imageName
                                }
                            }
                        }
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
        audioProcessor.start { level ->
            _audioLevel.value = level
            val isControlling = operatingMode == OperatingMode.OFFLINE || isPublishing

            if (isControlling) {
                val scaledLevel = level.pow(0.5f)
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
        }
    }

    private fun stopListening() {
        _isListening.value = false
        _audioLevel.value = 0f
        audioProcessor.stop()
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
        _activeState.value = dataManager.activePuppet.value?.states?.find { it.name == "idle" }
    }

    fun stopBlinking() {
        clientBlinkingJob?.cancel()
    }
}
