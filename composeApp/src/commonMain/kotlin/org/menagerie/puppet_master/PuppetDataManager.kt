package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

expect class PuppetDataManager(scope: CoroutineScope, context: Any) {
    val uploadsDir: String
    val troupe: StateFlow<PuppetTroupe?>
    val activePuppet: StateFlow<PuppetCharacter?>

    fun setOperatingMode(mode: OperatingMode)
    fun connectAndSync(serverIp: String)
    fun setActivePuppet(name: String)
    fun createNewPuppet(name: String)
    fun createNewState(
        stateName: String,
        imageBytes: ByteArray,
        localImageName: String,
        blinkImageBytes: ByteArray?,
        localBlinkImageName: String?,
        serverIp: String
    )
    fun updatePuppet(puppetName: String, update: (PuppetCharacter) -> PuppetCharacter)
    fun publishTroupe(serverIp: String)
}
