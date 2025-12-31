package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

/**
 * Represents a 2D coordinate, typically for mouse position in pixels, though this also represents an android tap/touch+drag action.
 *
 * @property x The horizontal coordinate.
 * @property y The vertical coordinate.
 */
@Serializable
data class MousePosition(val x: Int, val y: Int)

/**
 * Represents the calibration data for a screen area.
 * This defines a rectangular region used for mapping coordinates.
 *
 * @property topLeft The normalized coordinates of the top-left corner of the calibrated area.
 * @property bottomRight The normalized coordinates of the bottom-right corner of the calibrated area.
 */
@Serializable
data class CalibrationData(val topLeft: SerializableOffset, val bottomRight: SerializableOffset)

/**
 * Represents the visual animation properties of a puppet state.
 * This includes transformations like rotation, scaling, and translation, as well as visual effects like glow.
 *
 * @property rotation The rotation of the object in degrees.
 * @property scaleX The horizontal scaling factor.
 * @property scaleY The vertical scaling factor.
 * @property translationX The horizontal translation offset.
 * @property translationY The vertical translation offset.
 * @property pathTranslationX The horizontal translation offset from the follow path effect.
 * @property pathTranslationY The vertical translation offset from the follow path effect.
 * @property glowColor The ARGB color of the glow effect as an Int.
 * @property glowIntensity The intensity of the glow effect, where < 1.0 results in darkening.
 */
@Serializable
data class AnimationState(
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val pathTranslationX: Float = 0f,
    val pathTranslationY: Float = 0f,
    val glowColor: Int = 0xFFFFFFFF.toInt(),
    val glowIntensity: Float = 1f
)

/**
 * Represents the overall state sent from the server to the clients.
 * This encapsulates all the necessary information for a client to render the current puppet state,
 * including any active special effects and calibration data.
 *
 * @property puppetStateInfo Information about the base puppet state to be displayed.
 * @property calibrationData The calibration data for the screen.
 * @property animationState The current animation properties to be applied to the puppet state.
 * @property effectStartTime The server-side start time (in milliseconds) of the currently active special effect, used for synchronized animation.
 */
@Serializable
data class ServerState(
    val puppetStateInfo: PuppetStateInfo? = null,
    val calibrationData: CalibrationData? = null,
    val animationState: AnimationState? = null,
    val effectStartTime: Long? = null,
)
