package org.menagerie.puppet_master

import kotlinx.serialization.json.Json
import java.io.File

class TroupeManager {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val troupeFile = File("troupe.json")

    var troupe: PuppetTroupe? = null
        private set

    val activePuppet: PuppetCharacter?
        get() = troupe?.puppets?.find { it.name == troupe?.activePuppetName }

    init {
        troupe = loadTroupe()
    }

    fun saveTroupe(troupe: PuppetTroupe) {
        this.troupe = troupe
        troupeFile.writeText(json.encodeToString(PuppetTroupe.serializer(), troupe))
    }

    private fun loadTroupe(): PuppetTroupe? {
        if (troupeFile.exists()) {
            return try {
                json.decodeFromString(PuppetTroupe.serializer(), troupeFile.readText())
            } catch (e: Exception) {
                null
            }
        }
        return null
    }

    fun updateTroupe(newTroupe: PuppetTroupe) {
        saveTroupe(newTroupe)
    }
}