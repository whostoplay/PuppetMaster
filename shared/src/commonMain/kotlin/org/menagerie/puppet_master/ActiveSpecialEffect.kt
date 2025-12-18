package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Represents the currently active special effect and calculates its visual properties over time.
 * This class manages the animation state of a [SpecialEffect], such as scaling, rotation, and vibration,
 * based on the elapsed time since the effect started.
 *
 * @property effect The [SpecialEffect] data object that defines the effect's parameters.
 * @param startTime The time in milliseconds when the effect was activated. Defaults to the current system time.
 */
@Serializable
class ActiveSpecialEffect(
    val effect: SpecialEffect,
    private var startTime: Long = System.currentTimeMillis()
) {

    /**
     * Calculates the current horizontal scale factor, animated over time.
     * @return The calculated scale value for the X-axis.
     */
    fun getScaleX(): Float {
        return getAnimatedScale(effect.scaleX)
    }

    /**
     * Calculates the current vertical scale factor, animated over time.
     * @return The calculated scale value for the Y-axis.
     */
    fun getScaleY(): Float {
        return getAnimatedScale(effect.scaleY)
    }

    /**
     * Helper function to calculate the animated scale using a sine wave for a smooth pulsing effect.
     *
     * @param targetScale The target scale to animate towards.
     * @return The interpolated scale value at the current time.
     */
    private fun getAnimatedScale(targetScale: Float): Float {
        if (effect.scaleSpeed == 0f) {
            return targetScale
        }
        val elapsedTime = (System.currentTimeMillis() - startTime).toFloat()
        val angle = (elapsedTime / (MILLISECONDS_IN_SECOND / (effect.scaleSpeed + 1))) * 2 * PI
        // Animate from 1.0 to targetScale using a sine wave
        return 1f + (sin(angle.toFloat()) * (targetScale - 1f))
    }

    /**
     * Calculates the current rotation angle in degrees.
     * The rotation is linear based on the elapsed time and spin speed.
     *
     * @return The current rotation angle.
     */
    fun getRotation(): Float {
        if (effect.spinSpeed == 0f) {
            return 0f
        }
        val elapsedTime = System.currentTimeMillis() - startTime
        val spinDuration = ROTATION_DURATION_FACTOR / effect.spinSpeed
        val rotation = (elapsedTime / spinDuration) * DEGREES_IN_CIRCLE
        return rotation * effect.spinDirection
    }

    /**
     * Gets the intensity of the glow effect.
     * @return The glow intensity, or 0f if not defined.
     */
    fun getGlow(): Float {
        return effect.glowIntensity ?: 1f
    }

    /**
     * Gets the color of the glow effect.
     * @return The glow color as an ARGB Int, or white if not defined.
     */
    fun getGlowColor(): Int {
        return effect.glowColor ?: DEFAULT_GLOW_COLOR
    }

    /**
     * Calculates the current random offset for the vibration effect.
     * The offset is recalculated periodically based on the vibration speed, creating a shaking effect.
     *
     * @param maxOffset The maximum distance the object can be offset.
     * @return A [SerializableOffset] representing the calculated X and Y offsets.
     */
    fun getVibrationOffset(maxOffset: Float): SerializableOffset {
        if (effect.vibrationDistance == 0f) {
            return SerializableOffset(0f, 0f)
        }
        val elapsedTime = (System.currentTimeMillis() - startTime).toFloat()
        val offset = effect.vibrationDistance * maxOffset

        val vibrationDuration = MILLISECONDS_IN_SECOND / (effect.vibrationSpeed * VIBRATION_SPEED_MULTIPLIER + 1)
        val vibrationCycle = (elapsedTime / vibrationDuration).toInt()
        val random = Random(vibrationCycle.toLong()) // Use the cycle as a seed for consistent randomness within a cycle

        // Generate a random point in a unit circle
        val x = random.nextFloat() * 2f - 1f
        val y = random.nextFloat() * 2f - 1f

        // Normalize the vector to ensure consistent distance
        val magnitude = sqrt(x * x + y * y)
        val normalizedX = if (magnitude > 0f) x / magnitude else 0f
        val normalizedY = if (magnitude > 0f) y / magnitude else 0f

        return SerializableOffset(normalizedX * offset, normalizedY * offset)
    }

    /**
     * Creates a new [ActiveSpecialEffect] with the same start time as the current one.
     * This is useful for smoothly transitioning to a new effect while preserving the animation phase.
     *
     * @param newEffect The new [SpecialEffect] to apply.
     * @return A new [ActiveSpecialEffect] instance.
     */
    fun copyWithPreservedStartTime(newEffect: SpecialEffect): ActiveSpecialEffect {
        return ActiveSpecialEffect(newEffect, this.startTime)
    }

    companion object {
        private const val MILLISECONDS_IN_SECOND = 1000f
        private const val DEGREES_IN_CIRCLE = 360f
        private const val ROTATION_DURATION_FACTOR = 5000f
        private const val VIBRATION_SPEED_MULTIPLIER = 32
        private const val DEFAULT_GLOW_COLOR = 0xFFFFFFFF.toInt()
    }
}
