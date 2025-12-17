package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

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
        val angle = (elapsedTime / (1000f / (effect.scaleSpeed + 1))) * 2 * PI
        return 1f + (sin(angle.toFloat()) * (targetScale - 1f))
    }

    fun getRotation(): Float {
        if (effect.spinSpeed == 0f) {
            return 0f
        }
        val elapsedTime = System.currentTimeMillis() - startTime
        val spinDuration = 5000f / effect.spinSpeed
        val rotation = (elapsedTime / spinDuration) * 360f
        return rotation * effect.spinDirection
    }

    fun getGlow(): Float {
        return effect.glowIntensity ?: 0f
    }

    fun getGlowColor(): Int {
        return effect.glowColor ?: 0xFFFFFFFF.toInt()
    }

    fun getVibrationOffset(maxOffset: Float): SerializableOffset {
        if (effect.vibrationDistance == 0f) {
            return SerializableOffset(0f, 0f)
        }
        val elapsedTime = (System.currentTimeMillis() - startTime).toFloat()
        val offset = effect.vibrationDistance * maxOffset

        val vibrationDuration = 1000f / (effect.vibrationSpeed * 32 + 1)
        val vibrationCycle = (elapsedTime / vibrationDuration).toInt()
        val random = Random(vibrationCycle.toLong())

        val x = random.nextFloat() * 2f - 1f
        val y = random.nextFloat() * 2f - 1f

        val magnitude = sqrt(x * x + y * y)
        val normalizedX = if (magnitude > 0f) x / magnitude else 0f
        val normalizedY = if (magnitude > 0f) y / magnitude else 0f

        return SerializableOffset(normalizedX * offset, normalizedY * offset)
    }

    fun copyWithPreservedStartTime(newEffect: SpecialEffect): ActiveSpecialEffect {
        return ActiveSpecialEffect(newEffect, this.startTime)
    }
}