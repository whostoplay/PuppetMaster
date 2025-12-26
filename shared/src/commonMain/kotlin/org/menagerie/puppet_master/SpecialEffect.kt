package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

/**
 * Represents a special effect that can be applied to a puppet.
 *
 * @property name The name of the special effect.
 * @property vibrationDistance The distance the puppet vibrates.
 * @property vibrationSpeed The speed at which the puppet vibrates.
 * @property glowIntensity The intensity of the glow effect.
 * @property glowColor The color of the glow effect.
 * @property scaleX The horizontal scale of the puppet.
 * @property scaleY The vertical scale of the puppet.
 * @property scaleSpeed The speed at which the puppet scales.
 * @property spinSpeed The speed at which the puppet spins.
 * @property spinDirection The direction of the spin (1 for clockwise, -1 for counter-clockwise).
 */
@Serializable
data class SpecialEffect(
    val name: String = "New Effect",
    val vibrationDistance: Float = 0f,
    val vibrationSpeed: Float = 0f,
    val glowIntensity: Float? = 1f,
    val glowColor: Int? = 0xFFFFFFFF.toInt(),
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleSpeed: Float = 0f,
    val spinSpeed: Float = 0f,
    val spinDirection: Int = 1
)
