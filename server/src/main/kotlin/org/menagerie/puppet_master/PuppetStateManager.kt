package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.random.Random

class PuppetStateManager(private val scope: CoroutineScope, private val troupeManager: TroupeManager) {

    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    val activeState = _activeState.asStateFlow()

    private val _stateToSend = MutableStateFlow<ServerState?>(null)
    val stateToSend = _stateToSend.asStateFlow()

    private val _activeSpecialEffect = MutableStateFlow<ActiveSpecialEffect?>(null)

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

    fun onClientSentState(imageName: String) {
        val activePuppet = troupeManager.activePuppet ?: return
        val baseState = activePuppet.states.find { it.imageName == imageName || it.blinkImageName == imageName }
        if (baseState != null) {
            if (_activeState.value != baseState) {
                _activeState.value = baseState
            }
            if (_stateToSend.value?.imageName != imageName) {
                val tempState = baseState.copy(imageName = imageName)
                updateStateToSend(tempState)
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
        val effect = state?.appliedEffectName?.let { name ->
            troupeManager.troupe?.specialEffectsManager?.effects?.find { it.name == name }
        }

        if (effect != null) {
            val newEffect = ActiveSpecialEffect(effect)
            // We don't have the UI state here, so we can't preserve the start time
            _activeSpecialEffect.value = newEffect
        } else {
            _activeSpecialEffect.value = null
        }

        _stateToSend.value = ServerState(state?.imageName, _activeSpecialEffect.value)
    }
}
