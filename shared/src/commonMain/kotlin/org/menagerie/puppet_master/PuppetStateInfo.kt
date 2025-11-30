package org.menagerie.puppet_master

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class PuppetStateInfo(
    val name: String,
    val imageName: String,
    val blinkImageName: String? = null,
    val minBlinkRate: Long = 1500L,
    val maxBlinkRate: Long = 6000L,
    @Transient private val lastUpdated: Long = System.currentTimeMillis()
)
