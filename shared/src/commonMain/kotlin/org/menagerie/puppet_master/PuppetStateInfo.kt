package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

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
 */
@Serializable
data class PuppetStateInfo(
    val name: String,
    val imageName: String,
    val blinkImageName: String? = null,
    val minBlinkRate: Long = 100L,
    val maxBlinkRate: Long = 5000L,
    val appliedEffect: SpecialEffect? = null,
    val eyeState: EyeState? = null
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