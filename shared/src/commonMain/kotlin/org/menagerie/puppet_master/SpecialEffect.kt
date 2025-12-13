package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class SpecialEffect(
    val name: String = "New Effect",
    val vibrationDistance: Float = 0f,
    val vibrationSpeed: Float = 0f,
    val glowIntensity: Float? = 1f,
    val glowColor: Int? = 0xffffff,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleSpeed: Float = 0f,
    val spinSpeed: Float = 0f,
    val spinDirection: Int = 1
)
