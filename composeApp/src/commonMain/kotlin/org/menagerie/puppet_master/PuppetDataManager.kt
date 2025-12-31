package org.menagerie.puppet_master

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import org.menagerie.puppet_master.state_machine.NodeGraph

/**
 * Defines a common API for managing all puppet and troupe-related data in a multiplatform context.
 *
 * This `expect` class outlines the core functionalities for creating, updating, saving, loading, and
 * synchronizing puppet data. Platform-specific modules (`androidMain`, `jvmMain`) must provide `actual`
 * implementations that handle the concrete logic for file I/O, network requests, and platform-specific APIs.
 *
 * @param scope A [CoroutineScope] for launching long-running operations like network requests.
 * @param context A generic placeholder for a platform-specific context object (e.g., `android.content.Context`).
 */
expect class PuppetDataManager(scope: CoroutineScope, context: Any) {
    /**
     * The absolute path to the directory where images and other assets are stored locally.
     */
    val uploadsDir: String

    /**
     * A [StateFlow] that emits the currently loaded [PuppetTroupe], or null if no troupe is active.
     */
    val troupe: StateFlow<PuppetTroupe?>

    /**
     * A [StateFlow] that emits the currently active [PuppetCharacter], derived from the active troupe.
     */
    val activePuppet: StateFlow<PuppetCharacter?>

    /**
     * A [StateFlow] that emits the current [ConnectionState] of the data manager with a remote server.
     */
    val connectionState: StateFlow<ConnectionState>

    /**
     * Sets the data manager's operating mode (e.g., online or offline).
     * In [OperatingMode.ONLINE], changes may be synced with a server.
     * In [OperatingMode.OFFLINE], all operations are local.
     */
    fun setOperatingMode(mode: OperatingMode)

    /**
     * Connects to a server and synchronizes the local troupe data.
     * This can involve downloading, uploading, or merging data based on modification times.
     *
     * @param serverIp The IP address of the server to connect to.
     */
    fun connectAndSync(serverIp: String)

    /**
     * Sets the specified puppet as the active one in the current troupe.
     *
     * @param name The name of the puppet to set as active.
     */
    fun setActivePuppet(name: String)

    /**
     * Creates a new puppet character. If no troupe exists, a new one is created.
     *
     * @param name The name for the new puppet.
     * @param troupeName An optional name for a new troupe if one is created.
     */
    fun createNewPuppet(name: String, troupeName: String? = null)

    /**
     * Creates a new state for the active puppet, including its main and optional blink images.
     *
     * @param stateName The name of the state (e.g., "idle", "talking").
     * @param imageBytes The image data for the default state.
     * @param localImageName The filename for the default image.
     * @param blinkImageBytes Optional image data for the blinking variant.
     * @param localBlinkImageName Optional filename for the blink image.
     * @param serverIp The server IP address, used for uploading images in online mode.
     */
    fun createNewState(
        stateName: String,
        imageBytes: ByteArray,
        localImageName: String,
        blinkImageBytes: ByteArray?,
        localBlinkImageName: String?,
        serverIp: String
    )

    /**
     * Updates a specific puppet within the troupe using a transformation function.
     *
     * @param puppetName The name of the puppet to modify.
     * @param update A lambda that receives the current [PuppetCharacter] and returns an updated version.
     */
    fun updatePuppet(puppetName: String, update: (PuppetCharacter) -> PuppetCharacter)

    /**
     * Publishes the entire local troupe to the server, including all data and images.
     *
     * @param serverIp The IP address of the server.
     */
    fun publishTroupe(serverIp: String)

    /**
     * Retrieves the image data for a given image name, abstracting away platform-specific file access.
     *
     * @param imageName The name or identifier of the image.
     * @return A [ByteArray] containing the image data, or null if not found.
     */
    suspend fun getImageData(imageName: String): ByteArray?

    /**
     * Persists the provided [PuppetTroupe] to the default storage location.
     *
     * @param troupe The troupe to save.
     */
    fun saveTroupe(troupe: PuppetTroupe)

    /**
     * Saves an image's byte data to local storage.
     *
     * @param name The filename for the image.
     * @param data The image data.
     */
    fun saveImage(name: String, data: ByteArray)

    /**
     * Renames the current troupe.
     *
     * @param newName The new name for the troupe. Overwrites old .troupe files if name already exists.
     */
    fun renameTroupe(newName: String)

    /**
     * Renames the current puppet.
     *
     * @param newName the new name for the puppet. Must be Unique.
     */
    fun renamePuppet(newName: String)

    /**
     * Loads a troupe from a specific file path (e.g., a `.troupe` ZIP archive).
     *
     * @param filePath The path to the file.
     * @return The loaded [PuppetTroupe], or null on failure.
     */
    fun loadTroupeFromFile(filePath: String): PuppetTroupe?

    /**
     * Exports a single puppet to a file (e.g., a `.puppet` ZIP archive).
     *
     * @param puppetName The name of the puppet to export.
     * @param exportPath The destination path for the file.
     */
    fun exportPuppet(puppetName: String, exportPath: String)

    /**
     * Imports a puppet from a file and adds it to the current troupe.
     *
     * @param filePath The path to the `.puppet` file.
     * @param newTroupeName If no troupe is active, this name is used to create a new one.
     */
    fun importPuppet(filePath: String, newTroupeName: String? = null)

    /**
     * Saves the current troupe to a specific file path as a ZIP archive.
     *
     * @param filePath The destination path for the file.
     */
    fun saveTroupeAs(filePath: String)

    /**
     * Creates a new, empty troupe, clearing the current one.
     */
    fun createNewTroupe()

    /**
     * Reloads the last used troupe from persistent storage, typically on application startup.
     */
    fun reloadLastTroupe()

    /**
     * Updates the node graph for the current troupe.
     *
     * @param nodeGraph The new node graph.
     */
    fun updateNodeGraph(nodeGraph: NodeGraph)
}
