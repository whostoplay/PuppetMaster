package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
                    updateStateToSend(null, animationState, null) // Pass null to avoid overwriting the state
                    delay(16) // roughly 60 fps
                }
            }
        } else {
            // No effect, ensure animation is cleared
            updateStateToSend(_stateToSend.value?.puppetStateInfo, null, _stateToSend.value?.puppetStateInfo?.eyeState?.cursorPosition)
        }
    }

// In PuppetStateManager


    // Call this method once when the PuppetStateManager is initialized.
// For example, in its init block or a dedicated start() method.
    fun startBlinkingLoop() {
        // Prevent launching multiple jobs if called more than once.
        if (blinkingJob?.isActive == true) return

        blinkingJob = CoroutineScope(Dispatchers.Default).launch {
            while (true) {
                // Wait a random amount of time before the next blink.
                val delayTime = Random.nextLong(1500, 5000)
                delay(delayTime)

                // Capture the current state at this moment.
                val currentState = _activeState.value ?: continue

                // Check if the current state is one that *should* blink.
                if (isHeadless() && currentState.blinkImageName != null) {

                    // Use a NonCancellable block to ensure the blink completes.
                    withContext(NonCancellable) {
                        val blinkState = currentState.copy(imageName = currentState.blinkImageName!!)

                        // Send the blinking image
                        updateStateToSend(
                            blinkState,
                            _stateToSend.value?.animationState,
                            _stateToSend.value?.puppetStateInfo?.eyeState?.cursorPosition
                        )

                        delay(150)

                        // IMPORTANT: Revert to the CURRENT active state, which may have
                        // changed during the 150ms delay. This prevents getting stuck.
                        updateStateToSend(
                            _activeState.value,
                            _stateToSend.value?.animationState,
                            _stateToSend.value?.puppetStateInfo?.eyeState?.cursorPosition
                        )
                    }
                }
            }
        }
    }

    // You will also need a method to stop it when the manager is disposed.
    fun stopBlinkingLoop() {
        blinkingJob?.cancel()
        blinkingJob = null
    }

    // Now, updateStateAndBlinking becomes MUCH simpler.
// It no longer manages the blinking job at all.
    private fun updateStateAndBlinking(state: PuppetStateInfo?) {
        // Just update the state. The persistent blinkingJob will handle the rest.
        updateStateToSend(state, _stateToSend.value?.animationState, _stateToSend.value?.puppetStateInfo?.eyeState?.cursorPosition)
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

    fun onMousePositionChanged(mousePosition: SerializableOffset) {
        val currentState = _stateToSend.value
        updateStateToSend(currentState?.puppetStateInfo, currentState?.animationState, mousePosition)
    }

    fun onCalibrationReceived(calibrationData: CalibrationData) {
        val currentState = _stateToSend.value
        _stateToSend.value = currentState?.copy(calibrationData = calibrationData)
    }

    fun onClientSentState(receivedState: ServerState) {
        receivedState.puppetStateInfo?.eyeState?.cursorPosition?.let { onMousePositionChanged(it) }
        receivedState.calibrationData?.let { onCalibrationReceived(it) }

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
                onClientSentState(ServerState(newState))
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

    private fun updateStateToSend(state: PuppetStateInfo?, animationState: AnimationState?, mousePosition: SerializableOffset?) {
        val currentState = _stateToSend.value
        val currentInfo = currentState?.puppetStateInfo

        val newPuppetInfo = state ?: currentInfo
        val newAnimationState = animationState ?: currentState?.animationState

        val finalState = newPuppetInfo?.let {
            it.copy(
                eyeState = it.eyeState?.copy(
                    cursorPosition = mousePosition ?: it.eyeState?.cursorPosition
                )
            )
        }

        _stateToSend.value = ServerState(
            puppetStateInfo = finalState,
            calibrationData = currentState?.calibrationData,
            animationState = newAnimationState
        )
    }
}
