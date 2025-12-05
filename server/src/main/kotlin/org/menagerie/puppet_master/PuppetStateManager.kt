package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.random.Random

class PuppetStateManager(private val scope: CoroutineScope, private val troupeManager: TroupeManager) {

    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    val activeState = _activeState.asStateFlow()

    private val _stateToSend = MutableStateFlow<ServerState?>(null)
    val stateToSend = _stateToSend.asStateFlow()

    private var blinkingJob: Job? = null
    private var returnToIdleJob: Job? = null

    var manualControlActive = false
    var obsConnectionCount = 0
    private fun isHeadless() = obsConnectionCount > 0 && !manualControlActive

    init {
        _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" } ?: troupeManager.activePuppet?.states?.firstOrNull()

        scope.launch {
            activeState.collect { state ->
                blinkingJob?.cancel()
                updateStateToSend(state)
                if (state?.blinkImageName != null) {
                    blinkingJob = launch {
                        while (true) {
                            val delayTime = if (state.minBlinkRate >= state.maxBlinkRate) {
                                state.maxBlinkRate
                            } else {
                                Random.nextLong(state.minBlinkRate, state.maxBlinkRate)
                            }
                            delay(delayTime)
                            if (isHeadless() && _activeState.value == state) {
                                val blinkState = state.copy(imageName = state.blinkImageName!!)
                                updateStateToSend(blinkState)
                                delay(150)
                                updateStateToSend(state)
                            }
                        }
                    }
                }
            }
        }
    }

    fun onAudioLevelChanged(level: Float) {
        val activePuppet = troupeManager.activePuppet ?: return
        val scaledLevel = level.pow(0.5f)
        val sortedThresholds = activePuppet.thresholds.entries.sortedBy { it.key }
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

    fun onMousePositionChanged(mousePosition: MousePosition) {
        val currentState = _stateToSend.value
        _stateToSend.value = currentState?.copy(mousePosition = mousePosition)
    }

    fun onCalibrationReceived(calibrationData: CalibrationData) {
        val currentState = _stateToSend.value
        _stateToSend.value = currentState?.copy(calibrationData = calibrationData)
    }

    fun onClientSentState(stateJson: String) {
        val json = Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true }
        try {
            val receivedState = json.decodeFromString<ServerState>(stateJson)
            // Preserve mouse and calibration data from the current state
            _stateToSend.value = _stateToSend.value?.copy(puppetStateInfo = receivedState.puppetStateInfo) ?: receivedState

            // Also update the internal active state for blinking logic
            receivedState.puppetStateInfo?.let { stateInfo ->
                val baseState = troupeManager.activePuppet?.states?.find { it.imageName == stateInfo.imageName || it.blinkImageName == stateInfo.imageName }
                if (baseState != null && _activeState.value != baseState) {
                    _activeState.value = baseState
                }
            }
        } catch (e: Exception) {
            // It's possible the client is just sending an imageName as a string.
            val imageName = stateJson
            val activePuppet = troupeManager.activePuppet ?: return
            val baseState = activePuppet.states.find { it.imageName == imageName || it.blinkImageName == imageName }
            if (baseState != null) {
                if (_activeState.value != baseState) {
                    _activeState.value = baseState
                }
                if (_stateToSend.value?.puppetStateInfo?.imageName != imageName) {
                    val tempState = baseState.copy(imageName = imageName)
                    updateStateToSend(tempState)
                }
            }
        }
    }

    fun onTroupeUpdated() {
        if (!isHeadless()) {
            _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" } ?: troupeManager.activePuppet?.states?.firstOrNull()
        }
    }

    fun onClientDisconnected() {
        manualControlActive = false
        _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" } ?: troupeManager.activePuppet?.states?.firstOrNull()
    }

    private fun returnToIdle() {
        if (_activeState.value?.name != "idle") {
            returnToIdleJob?.cancel()
            returnToIdleJob = scope.launch {
                delay(100)
                _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" }
            }
        }
    }

    private fun updateStateToSend(state: PuppetStateInfo?) {
        val currentData = _stateToSend.value
        _stateToSend.value = ServerState(
            puppetStateInfo = state,
            mousePosition = currentData?.mousePosition,
            calibrationData = currentData?.calibrationData
        )
    }
}
