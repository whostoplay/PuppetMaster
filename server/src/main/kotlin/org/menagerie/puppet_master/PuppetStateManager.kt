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

/**
 * Manages the overall state of the active puppet, including its visual appearance, animations, and blinking.
 *
 * This class operates in two primary modes:
 * 1.  **Headless Mode:** Automated control based on audio input levels. The puppet's state changes
 *     dynamically in response to audio. Blinking is handled automatically. This mode is active
 *     when `manualControlActive` is false and `obsConnectionCount` > 0.
 * 2.  **Manual Control Mode:** A client application dictates the puppet's state. This class receives
 *     state information from the client and applies it.
 *
 * It exposes a [stateToSend] flow that emits the final [ServerState] to be broadcast to clients.
 *
 * @param scope The CoroutineScope to launch long-running jobs like blinking and animations.
 * @param troupeManager The manager for accessing puppet and troupe data.
 */
class PuppetStateManager(private val scope: CoroutineScope, private val troupeManager: TroupeManager) {

    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    private val activeState = _activeState.asStateFlow()

    private val _stateToSend = MutableStateFlow<ServerState?>(null)
    /**
     * A flow that emits the current [ServerState] to be sent to connected clients.
     * This includes puppet appearance, animation, eye position, and calibration data.
     */
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
            // No effect, ensure animation is cleared by calculating the default state.
            val animationState = calculateAnimationState()
            updateStateToSend(_stateToSend.value?.puppetStateInfo, animationState, _stateToSend.value?.puppetStateInfo?.eyeState?.cursorPosition)
        }
    }

    /**
     * Starts a persistent coroutine that handles automatic blinking for the puppet.
     *
     * In headless mode, if the current state is configured for blinking, this loop will
     * periodically switch to the blink image for a short duration and then revert.
     * It's designed to be called once upon initialization.
     */
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

    /**
     * Stops the automatic blinking loop coroutine.
     * Should be called when the manager is being disposed to prevent leaks.
     */
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



    private fun calculateAnimationState(): AnimationState {
        val effect = activeSpecialEffect ?: return AnimationState()
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

    /**
     * Updates the puppet's state based on the provided audio level.
     *
     * This is only active in headless mode. It compares the audio level against the
     * active puppet's defined thresholds and sets the corresponding state. If the level
     * drops below all thresholds, it schedules a return to the "idle" state.
     *
     * @param level The current audio input level, typically normalized.
     */
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

    /**
     * Updates the cursor position for the puppet's eye tracking.
     * @param mousePosition The new position of the cursor/mouse.
     */
    fun onMousePositionChanged(mousePosition: SerializableOffset) {
        val currentState = _stateToSend.value
        updateStateToSend(currentState?.puppetStateInfo, currentState?.animationState, mousePosition)
    }

    /**
     * Updates the calibration data for the puppet.
     * @param calibrationData The new calibration data.
     */
    fun onCalibrationReceived(calibrationData: CalibrationData) {
        val currentState = _stateToSend.value
        _stateToSend.value = currentState?.copy(calibrationData = calibrationData)
    }

    /**
     * Processes a [ServerState] object received from a client in manual control mode.
     * This updates the puppet's appearance, eye position, and special effects based on the client's input.
     * @param receivedState The state received from the client.
     */
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

    /**
     * Handles updates to the underlying troupe or puppet data.
     *
     * If in manual control, it attempts to find the equivalent of the current state in the new
     * troupe data. If not found, it disconnects the client.
     * If in headless mode, it resets the state to the new troupe's "idle" state.
     */
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

    /**
     * Resets the manager to headless mode when a client disconnects.
     * This deactivates manual control and sets the puppet state back to "idle".
     */
    fun onClientDisconnected() {
        manualControlActive = false
        updateSpecialEffect(null)
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
        val newAnimationState = animationState

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
