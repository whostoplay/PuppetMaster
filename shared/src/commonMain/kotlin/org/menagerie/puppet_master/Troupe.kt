package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class Puppet(
    val name: String,
    val lastUpdated: Long,
    val states: List<PuppetStateInfo>,
    val thresholds: Map<Float, PuppetStateInfo> = emptyMap()
)

@Serializable
data class Troupe(val activePuppetName: String, val puppets: List<Puppet>)
