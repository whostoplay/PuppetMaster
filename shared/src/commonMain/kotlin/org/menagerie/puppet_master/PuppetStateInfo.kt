package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class PuppetStateInfo(val name: String, val imageName: String)
