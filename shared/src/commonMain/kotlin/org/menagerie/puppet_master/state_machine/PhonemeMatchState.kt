package org.menagerie.puppet_master.state_machine

data class PhonemeMatchState(
    val confidence: Int = 0,
    val isActive: Boolean = false
)
