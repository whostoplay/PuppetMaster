package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class AvatarStateInfo(val name: String, val imageName: String)
