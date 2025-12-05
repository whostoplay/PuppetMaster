package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class MousePosition(val x: Int, val y: Int)

@Serializable
data class CalibrationData(val topLeft: MousePosition, val bottomRight: MousePosition)

@Serializable
data class ServerState(
    val puppetStateInfo: PuppetStateInfo?,
    val mousePosition: MousePosition? = null,
    val calibrationData: CalibrationData? = null
)
