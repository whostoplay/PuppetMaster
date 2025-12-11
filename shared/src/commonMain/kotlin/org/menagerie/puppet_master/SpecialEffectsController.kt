package org.menagerie.puppet_master

import kotlin.math.PI
import kotlin.math.sin

class SpecialEffectsController {

    fun getAnimatedScale(targetScale: Float, scaleSpeed: Float, elapsedTime: Long): Float {
        return 1f + (sin((elapsedTime / (1000f / (scaleSpeed + 1f))) * 2f * PI) * (targetScale - 1f)).toFloat()
    }

    fun getRotation(spinSpeed: Float, spinDirection: Int, elapsedTime: Long): Float {
        return if (spinSpeed > 0) {
            val spinDuration = 5000f / spinSpeed
            (elapsedTime / spinDuration) * 360f * spinDirection
        } else {
            0f
        }
    }

    fun getVibration(vibrationDistance: Float, vibrationSpeed: Float, elapsedTime: Long, width: Float): Pair<Float, Float> {
        return if (vibrationDistance > 0) {
            val offset = vibrationDistance * (width / 20f)
            val angle = (elapsedTime / (1000f / (vibrationSpeed * 2f + 1f))) * 2f * PI
            val x = sin(angle).toFloat() * offset
            val y = kotlin.math.cos(angle).toFloat() * offset
            Pair(x, y)
        } else {
            Pair(0f, 0f)
        }
    }
}
