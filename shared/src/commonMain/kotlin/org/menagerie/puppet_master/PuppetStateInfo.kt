package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class PuppetStateInfo(
    val name: String,
    val imageName: String,
    val blinkImageName: String? = null,
    val minBlinkRate: Long = 1500L,
    val maxBlinkRate: Long = 6000L
)
