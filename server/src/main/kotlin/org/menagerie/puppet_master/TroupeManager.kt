package org.menagerie.puppet_master

import kotlinx.serialization.json.Json
import java.io.File

class TroupeManager {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
    }
    private val troupeFile = File("troupe.json")
    private val legacyConfigFile = File("puppet_config.json")

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

        if (legacyConfigFile.exists()) {
            return try {
                val legacyConfig = json.decodeFromString(PuppetConfiguration.serializer(), legacyConfigFile.readText())
                val defaultPuppet = PuppetCharacter("Default", legacyConfig.lastUpdated, legacyConfig.states)
                val troupe = PuppetTroupe("Default", listOf(defaultPuppet))
                saveTroupe(troupe)
                legacyConfigFile.delete()
                troupe
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