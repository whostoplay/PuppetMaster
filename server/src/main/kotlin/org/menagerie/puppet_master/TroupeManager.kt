package org.menagerie.puppet_master

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Manages the loading, saving, and accessing of puppet troupe data.
 *
 * This class is responsible for persistence of the [PuppetTroupe] configuration to a JSON file (`troupe.json`)
 * within an `uploads` directory. It provides access to the currently loaded troupe and the active puppet.
 *
 * @constructor Creates a new TroupeManager and loads the troupe data from disk.
 */
class TroupeManager {

    private val json = Json {
        prettyPrint = true
        isLenient = true
        ignoreUnknownKeys = true
        encodeDefaults = true
        allowStructuredMapKeys = true
    }
    private val troupeDir = File("uploads").apply { mkdirs() }
    private val troupeFile = File(troupeDir, "troupe.json")

    /**
     * The currently loaded puppet troupe. Can be null if no troupe has been loaded or created.
     */
    var troupe: PuppetTroupe? = null
        private set

    /**
     * The currently active [PuppetCharacter] based on the `activePuppetName` in the troupe.
     * Returns null if no troupe is loaded or if the active puppet name doesn't match any puppet in the troupe.
     */
    val activePuppet: PuppetCharacter?
        get() = troupe?.puppets?.find { it.name == troupe?.activePuppetName }

    init {
        troupe = loadTroupe()
    }

    /**
     * Saves the provided [PuppetTroupe] to the `troupe.json` file.
     * This overwrites the existing file.
     *
     * @param troupe The [PuppetTroupe] to save.
     */
    fun saveTroupe(troupe: PuppetTroupe) {
        this.troupe = troupe
        troupeFile.writeText(json.encodeToString(PuppetTroupe.serializer(), troupe))
    }

    private fun loadTroupe(): PuppetTroupe? {
        if (troupeFile.exists()) {
            return try {
                json.decodeFromString(PuppetTroupe.serializer(), troupeFile.readText())
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
        return null
    }

    /**
     * Updates the current troupe and saves it to disk.
     * This is a convenience function that simply calls [saveTroupe].
     *
     * @param newTroupe The new [PuppetTroupe] data to save.
     */
    fun updateTroupe(newTroupe: PuppetTroupe) {
        saveTroupe(newTroupe)
    }
}