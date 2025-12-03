package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class ServerState(
    val puppetStateInfo: PuppetStateInfo?
)
