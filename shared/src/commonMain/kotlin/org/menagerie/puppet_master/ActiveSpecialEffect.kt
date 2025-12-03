package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlin.math.sin

@Serializable
data class SerializableOffset(val x: Float, val y: Float)

/**
 * Represents the currently active special effect, and calculates its visual properties over time.
 */
@Serializable
class ActiveSpecialEffect(
    val effect: SpecialEffect,
    private var startTime: Long = System.currentTimeMillis()
) {

    fun getScaleX(): Float {
        return getAnimatedScale(effect.scaleX)
    }

    fun getScaleY(): Float {
        return getAnimatedScale(effect.scaleY)
    }

    private fun getAnimatedScale(targetScale: Float): Float {
        if (effect.scaleSpeed == 0f) {
            return targetScale
        }
        val elapsedTime = (System.currentTimeMillis() - startTime).toFloat()
        val angle = (elapsedTime / (1000f / (effect.scaleSpeed + 1))) * 2 * Math.PI
        return 1f + (sin(angle).toFloat() * (targetScale - 1f))
    }

    fun getRotation(): Float {
        if (effect.spinSpeed == 0f) {
            return 0f
        }
        val elapsedTime = System.currentTimeMillis() - startTime
        val rotation = (elapsedTime * effect.spinSpeed * 10f / 50f) % 360f
        return rotation * effect.spinDirection
    }

    fun getGlow(): Float {
        return effect.glowIntensity
    }

    fun getGlowColor(): Int {
        return effect.glowColor
    }

    fun getVibrationOffset(maxOffset: Float): SerializableOffset {
        if (effect.vibrationDistance == 0f) {
            return SerializableOffset(0f, 0f)
        }
        val elapsedTime = (System.currentTimeMillis() - startTime).toFloat()
        val offset = effect.vibrationDistance * maxOffset
        val angle = (elapsedTime / (100f / (effect.vibrationSpeed + 1))) * 2 * Math.PI
        val x = (sin(angle) * offset).toFloat()
        val y = (sin(angle * 2) * offset).toFloat() // Using a different frequency for y to make it more interesting
        return SerializableOffset(x, y)
    }

    fun preserveStartTime(previousEffect: ActiveSpecialEffect?) {
        previousEffect?.let {
            startTime = it.startTime
        }
    }
}
