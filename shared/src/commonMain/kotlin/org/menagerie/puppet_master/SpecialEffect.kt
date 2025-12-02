package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class SpecialEffect(
    var name: String = "New Effect",
    var vibrationDistance: Float = 0f,
    var vibrationSpeed: Float = 0f,
    var glowIntensity: Float = 0f,
    var scaleX: Float = 1f,
    var scaleY: Float = 1f,
    var scaleSpeed: Float = 0f,
    var spinSpeed: Float = 0f,
    var spinDirection: Int = 1
)
