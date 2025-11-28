package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class AvatarConfiguration(
    val lastUpdated: Long,
    val states: List<AvatarStateInfo>
)
