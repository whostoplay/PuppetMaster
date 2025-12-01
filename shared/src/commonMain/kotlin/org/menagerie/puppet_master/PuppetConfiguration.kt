package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

/**
 * Represents the configuration for a puppet, including all of its possible states.
 *
 * @property lastUpdated The last time the configuration was updated, in milliseconds since the epoch.
 * @property states A list of all the possible states for the puppet.
 */
@Serializable
data class PuppetConfiguration(
    val lastUpdated: Long,
    val states: List<PuppetStateInfo>
)
