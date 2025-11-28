package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class PuppetConfiguration(
    val lastUpdated: Long,
    val states: List<PuppetStateInfo>
)
