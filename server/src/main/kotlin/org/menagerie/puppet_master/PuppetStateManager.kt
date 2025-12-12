package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.pow
import kotlin.random.Random

class PuppetStateManager(private val scope: CoroutineScope, private val troupeManager: TroupeManager) {

    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    private val activeState = _activeState.asStateFlow()

    private val _stateToSend = MutableStateFlow<ServerState?>(null)
    val stateToSend = _stateToSend.asStateFlow()

    private var blinkingJob: Job? = null
    private var returnToIdleJob: Job? = null
    private var animationJob: Job? = null

    private var activeSpecialEffect: ActiveSpecialEffect? = null

    var manualControlActive = false
    var obsConnectionCount = 0
    private fun isHeadless() = obsConnectionCount > 0 && !manualControlActive

    init {
        scope.launch {
            activeState.collect { state ->
                // This collector is for headless mode ONLY
                if (manualControlActive) return@collect

                // Manage special effect
                val newEffect = state?.appliedEffect
                if (newEffect != activeSpecialEffect?.effect) {
                    updateSpecialEffect(state)
                }

                // Update state and start blinking if needed
                updateStateAndBlinking(state)
            }
        }
        // Set initial state for headless mode
        _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" }
    }

    // Manages the animation loop based on an effect.
    private fun updateSpecialEffect(state: PuppetStateInfo?, startTime: Long? = null) {
        animationJob?.cancel()

        activeSpecialEffect = state?.appliedEffect?.let { ActiveSpecialEffect(it, startTime ?: System.currentTimeMillis()) }

        if (activeSpecialEffect != null) {
            animationJob = scope.launch {
                while (true) {
                    val animationState = calculateAnimationState()
                    updateStateToSend(state, animationState)
                    delay(16) // roughly 60 fps
                }
            }
        } else {
            // No effect, ensure animation is cleared
            updateStateToSend(_stateToSend.value?.puppetStateInfo, null)
        }
    }

    // Updates the puppet state info and restarts the blinking loop if necessary.
    private fun updateStateAndBlinking(state: PuppetStateInfo?) {
        blinkingJob?.cancel()

        // Send the main state update
        updateStateToSend(state, calculateAnimationState())

        if (state?.blinkImageName != null) {
            blinkingJob = scope.launch {
                while (true) {
                    val delayTime = if (state.minBlinkRate >= state.maxBlinkRate) {
                        state.maxBlinkRate
                    } else {
                        Random.nextLong(state.minBlinkRate, state.maxBlinkRate)
                    }
                    delay(delayTime)
                    // In headless mode, if the state is still the active one, perform a blink
                    if (isHeadless() && _activeState.value == state) {
                        val blinkState = state.copy(imageName = state.blinkImageName!!)
                        updateStateToSend(blinkState, _stateToSend.value?.animationState)
                        delay(150)
                        updateStateToSend(state, _stateToSend.value?.animationState)
                    }
                }
            }
        }
    }


    private fun calculateAnimationState(): AnimationState? {
        val effect = activeSpecialEffect ?: return null
        val offset = effect.getVibrationOffset(1920f / 20f)
        return AnimationState(
            rotation = effect.getRotation(),
            scaleX = effect.getScaleX(),
            scaleY = effect.getScaleY(),
            translationX = offset.x,
            translationY = offset.y,
            glowColor = effect.getGlowColor(),
            glowIntensity = effect.getGlow()
        )
    }

    fun onAudioLevelChanged(level: Float) {
        if (manualControlActive) return // Ignore audio when client is in control

        val activePuppet = troupeManager.activePuppet ?: return
        val scaledLevel = level.pow(0.5f)
        val sortedThresholds = activePuppet.thresholds.entries.sortedBy { it.key }
        val activeThresholdIndex = sortedThresholds.indexOfLast { scaledLevel >= it.key }

        if (activeThresholdIndex != -1) {
            returnToIdleJob?.cancel()
            var state: PuppetStateInfo? = null
            // Find the highest-threshold state that is not null
            for (i in activeThresholdIndex downTo 0) {
                if (sortedThresholds[i].value != null) {
                    state = sortedThresholds[i].value
                    break
                }
            }

            if (state != null) {
                if (_activeState.value != state) {
                    _activeState.value = state
                }
            } else {
                // No state found for any threshold below the current level
                returnToIdle()
            }
        } else {
            // Audio level is below all thresholds
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
        val receivedState = try {
            json.decodeFromString<ServerState>(stateJson)
        } catch (e: Exception) {
            val imageName = stateJson
            val state = troupeManager.activePuppet?.states?.find { it.imageName == imageName || it.blinkImageName == imageName }
            ServerState(state)
        }

        val stateInfo = receivedState.puppetStateInfo
        if (stateInfo != null) {
            if (stateInfo.appliedEffect != activeSpecialEffect?.effect) {
                updateSpecialEffect(stateInfo, receivedState.effectStartTime)
            }
            updateStateAndBlinking(stateInfo)
        }
    }

    fun onTroupeUpdated() {
        // When troupe data changes, re-evaluate the current state.
        if (manualControlActive) {
            // If client is in control, find the new version of the current state and apply it
            val currentStateSentByClient = _stateToSend.value?.puppetStateInfo ?: return
            val newState = troupeManager.activePuppet?.states?.find { it.name == currentStateSentByClient.name }

            if (newState != null) {
                val json = Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true; classDiscriminator = "type" }
                onClientSentState(json.encodeToString(ServerState(newState)))
            } else {
                onClientDisconnected()
            }
        } else {
            // If in headless mode, reset to the new troupe's idle state.
            _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" }
        }
    }

    fun onClientDisconnected() {
        manualControlActive = false
        // Headless mode should take over, reset to idle.
        _activeState.value = troupeManager.activePuppet?.states?.find { it.name == "idle" }
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

    private fun updateStateToSend(state: PuppetStateInfo?, animationState: AnimationState?) {
        val currentData = _stateToSend.value
        _stateToSend.value = ServerState(
            puppetStateInfo = state,
            mousePosition = currentData?.mousePosition,
            calibrationData = currentData?.calibrationData,
            animationState = animationState
        )
    }
}
