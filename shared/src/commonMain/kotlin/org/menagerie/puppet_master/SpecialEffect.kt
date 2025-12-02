package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class SpecialEffect(
    var name: String = "New Effect",
    var vibrationIntensity: Float = 0f,
    var glowIntensity: Float = 0f,
    var scale: Float = 1f,
    var spinSpeed: Float = 0f
)
