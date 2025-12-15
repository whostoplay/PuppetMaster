package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

expect class PuppetDataManager(scope: CoroutineScope, context: Any) {
    val uploadsDir: String
    val troupe: StateFlow<PuppetTroupe?>
    val activePuppet: StateFlow<PuppetCharacter?>
    val connectionState: StateFlow<ConnectionState>

    fun setOperatingMode(mode: OperatingMode)
    fun connectAndSync(serverIp: String)
    fun setActivePuppet(name: String)
    fun createNewPuppet(name: String, troupeName: String? = null)
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
    suspend fun getImageData(imageName: String): ByteArray?
    fun saveTroupe(troupe: PuppetTroupe)
    fun saveImage(name: String, data: ByteArray)
    fun renameTroupe(newName: String)
    fun loadTroupeFromFile(filePath: String): PuppetTroupe?
    fun exportPuppet(puppetName: String, exportPath: String)
    fun importPuppet(filePath: String, newTroupeName: String? = null)
    fun saveTroupeAs(filePath: String)
    fun createNewTroupe()
}
