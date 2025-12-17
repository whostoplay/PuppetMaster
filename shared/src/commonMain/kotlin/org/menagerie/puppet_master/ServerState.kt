package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class MousePosition(val x: Int, val y: Int)

@Serializable
data class CalibrationData(val topLeft: SerializableOffset, val bottomRight: SerializableOffset)

@Serializable
data class AnimationState(
    val rotation: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val translationX: Float = 0f,
    val translationY: Float = 0f,
    val glowColor: Int = 0xFFFFFFFF.toInt(),
    val glowIntensity: Float = 0f
)

@Serializable
data class ServerState(
    val puppetStateInfo: PuppetStateInfo? = null,
    val calibrationData: CalibrationData? = null,
    val animationState: AnimationState? = null,
    val effectStartTime: Long? = null,
)
