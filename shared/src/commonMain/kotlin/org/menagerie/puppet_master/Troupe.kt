package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

/**
 * Represents a single puppet character, with its own set of states and audio thresholds.
 *
 * @property name The name of the puppet character.
 * @property lastUpdated The last time this puppet character was updated.
 * @property states A list of all possible states for this puppet character.
 * @property thresholds A map of audio level thresholds to puppet states. When the audio level exceeds a threshold, the puppet will switch to the corresponding state.
 * @property stateHotkeys A map of hotkeys to puppet state names. When a hotkey is pressed, the puppet will switch to the corresponding state.
 */
@Serializable
data class PuppetCharacter(
    val name: String,
    val lastUpdated: Long,
    val states: List<PuppetStateInfo>,
    val thresholds: Map<Float, PuppetStateInfo> = emptyMap(),
    val stateHotkeys: Map<Hotkey, String> = emptyMap()
)

/**
 * Represents a troupe of puppet characters.
 *
 * @property activePuppetName The name of the currently active puppet character.
 * @property puppets A list of all puppet characters in the troupe.
 * @property specialEffectsManager The manager for all special effects in the troupe.
 */
@Serializable
data class PuppetTroupe(
    val activePuppetName: String, 
    val puppets: List<PuppetCharacter>,
    val specialEffectsManager: SpecialEffectsManager
)
