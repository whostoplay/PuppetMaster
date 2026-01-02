package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * Represents a single layer to be rendered on top of a puppet's base image.
 *
 * @property imageName The name of the image for this layer.
 * @property position The offset from the puppet's center.
 * @property scaleX The horizontal scale of the layer.
 * @property scaleY The vertical scale of the layer.
 */
@Serializable
data class Layer(
    val imageName: String,
    val position: SerializableOffset = SerializableOffset(0f, 0f),
    val scaleX: Float = .5f,
    val scaleY: Float = .5f
)

/**
 * Represents the configuration for a single state of a puppet.
 *
 * @property name The name of the state.
 * @property imageName The name of the image to display for this state.
 * @property blinkImageName The name of the image to display when the puppet blinks. If null, the puppet will not blink.
 * @property minBlinkRate The minimum time in milliseconds between blinks.
 * @property maxBlinkRate The maximum time in milliseconds between blinks.
 * @property appliedEffect The special effect applied to this state.
 * @property eyeState The eye state for this puppet state.
 * @property layers A list of additional layers to render on top of the base image.
 * @property hotkey The hotkey associated with this state.
 */
@Serializable
data class PuppetStateInfo(
    val name: String,
    val imageName: String,
    val blinkImageName: String? = null,
    val minBlinkRate: Long = 100L,
    val maxBlinkRate: Long = 5000L,
    val appliedEffect: SpecialEffect? = null,
    val eyeState: EyeState? = null,
    val layers: List<Layer> = emptyList(),
    val hotkey: Hotkey? = null
) {
    @Transient
    private var lastUpdated: Long = System.currentTimeMillis()

    /**
     * Updates the lastUpdated timestamp to the current time.
     */
    fun updateTimestamp() {
        lastUpdated = System.currentTimeMillis()
    }

    /**
     * Returns the last time the state was updated.
     */
    fun getLastUpdated(): Long {
        return lastUpdated
    }
}