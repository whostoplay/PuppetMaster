package org.menagerie.puppet_master

import io.ktor.client.* 
import io.ktor.client.call.* 
import io.ktor.client.plugins.* 
import io.ktor.client.plugins.contentnegotiation.* 
import io.ktor.client.request.* 
import io.ktor.client.statement.bodyAsText
import io.ktor.http.* 
import io.ktor.serialization.kotlinx.json.* 
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.menagerie.puppet_master.state_machine.NodeGraph
import org.menagerie.puppet_master.state_machine.nodeSerializersModule
import puppetmaster.composeapp.generated.resources.Res
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Manages puppet data, including troupes, puppets, and states.
 * Handles local storage, synchronization with a server, and import/export operations.
 * @param scope The coroutine scope for launching background tasks.
 * @param context The application context.
 */
actual class PuppetDataManager actual constructor(private val scope: CoroutineScope, private val context: Any) {

    /**
     * The directory where uploaded files are stored.
     */
    actual val uploadsDir = getUploadsDir(context)
    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true; isLenient = true; allowStructuredMapKeys = true; serializersModule = nodeSerializersModule }) }
        install(HttpTimeout) { 
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
    }
    private val uploader = Uploader(client)
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true; allowStructuredMapKeys = true; serializersModule = nodeSerializersModule }
    private val settingsRepository = SettingsRepository(context)

    private val _troupe = MutableStateFlow<PuppetTroupe?>(null)
    /**
     * The current puppet troupe.
     */
    actual val troupe: StateFlow<PuppetTroupe?> = _troupe

    private val _activePuppet = MutableStateFlow<PuppetCharacter?>(null)
    /**
     * The currently active puppet in the troupe.
     */
    actual val activePuppet: StateFlow<PuppetCharacter?> = _activePuppet

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    /**
     * The current connection state to the server.
     */
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private var operatingMode: OperatingMode = OperatingMode.OFFLINE

    init {
        reloadLastTroupe()
    }

    /**
     * Reloads the last used troupe from settings.
     */
    actual fun reloadLastTroupe() {
        scope.launch {
            val settings = settingsRepository.loadSettings()
            val troupeFile = settings.lastTroupeFile?.let { File(it) }

            val localTroupe = if (troupeFile?.exists() == true) {
                loadTroupeFromFile(troupeFile.absolutePath)
            } else {
                val mostRecentTroupe = File(System.getProperty("user.dir")).listFiles { _, name -> name.endsWith(".troupe") }?.maxByOrNull { it.lastModified() }
                if (mostRecentTroupe != null) {
                    loadTroupeFromFile(mostRecentTroupe.absolutePath)
                } else {
                    createDefaultTroupe()
                }
            }

            if (localTroupe != null) {
                _troupe.value = localTroupe
                _activePuppet.value = localTroupe.puppets.find { it.name == localTroupe.activePuppetName }
                saveTroupe(localTroupe)
            }
        }
    }

    private suspend fun createDefaultTroupe(): PuppetTroupe {
        extractDefaultImages()

        val leftEye = Eye(
            openState = "iris.png",
            closedState = "blink.png",
            pupil = "pupil.png",
            position = SerializableOffset(x = 200.99791f, y = 314.03503f),
            scaleX = 0.31122905f,
            scaleY = 0.22924685f,
            maxPupilRadiusX = 49.283577f,
            maxPupilRadiusY = 60.261063f
        )

        val rightEye = Eye(
            openState = "iris.png",
            closedState = "blink.png",
            pupil = "pupil.png",
            position = SerializableOffset(x = 279.34973f, y = 313.36972f),
            scaleX = 0.28802267f,
            scaleY = 0.19401929f,
            maxPupilRadiusX = 46.954967f,
            maxPupilRadiusY = 55.603954f
        )

        val eyeState = EyeState(
            stateName = "idle",
            eyes = EyePair(
                left = leftEye,
                right = rightEye,
                followCursor = true,
                focusOnGame = false,
                gameScreenLocation = SerializableOffset(0.5f, 0.5f),
                checkOnAudience = true,
                audienceCheckRate = 8000L,
                audienceCheckDuration = 1500L
            )
        )

        val screamEffect = SpecialEffect(
            name = "Scream",
            vibrationSpeed = 1f,
            vibrationDistance = 1f,
        )

        val idleState = PuppetStateInfo(
            name = "idle",
            imageName = "icon_rough_closed.png",
            eyeState = eyeState
        )

        val talkState = PuppetStateInfo(
            name = "talking",
            imageName = "icon_rough.png",
            eyeState = eyeState
        )

        val yellState = PuppetStateInfo(
            name = "Yell",
            imageName = "icon_rough.png",
            eyeState = eyeState,
            appliedEffect = screamEffect
        )

        val defaultPuppet = PuppetCharacter(
            name = "Skulli",
            lastUpdated = System.currentTimeMillis(),
            states = listOf(idleState, talkState, yellState),
            thresholds = mapOf(0.1f to talkState, 0.175f to yellState)
        )

        return PuppetTroupe(
            name = "Menagerie",
            activePuppetName = "Skulli",
            puppets = listOf(defaultPuppet),
            specialEffectsManager = SpecialEffectsManager(
                effects = listOf(screamEffect),
                activeEffectIndex = 0
            )
        )
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun extractDefaultImages() {
        val defaultImages = listOf(
            "icon_rough.png",
            "icon_rough_closed.png",
            "blink.png",
            "iris.png",
            "pupil.png"
        )

        defaultImages.forEach { imageName ->
            val imageFile = File(uploadsDir, imageName)
            if (!imageFile.exists()) {
                try {
                    val resourcePath = "drawable/$imageName"
                    val bytes = Res.readBytes(resourcePath)
                    imageFile.writeBytes(bytes)
                } catch (e: Exception) {
                    println("Error extracting default image $imageName: ${e.message}")
                }
            }
        }
    }

    /**
     * Gets the image data for a given image name.
     * @param imageName The name of the image to retrieve.
     * @return The image data as a byte array, or null if the image is not found.
     */
    actual suspend fun getImageData(imageName: String): ByteArray? {
        val file = File(uploadsDir, imageName)
        return if (file.exists() && file.isFile) file.readBytes() else null
    }

    /**
     * Sets the operating mode (online or offline).
     * @param mode The new operating mode.
     */
    actual fun setOperatingMode(mode: OperatingMode) {
        operatingMode = mode
    }

    /**
     * Saves the current troupe.
     * If in online mode, it also publishes the troupe to the server.
     * @param troupe The troupe to save.
     */
    actual fun saveTroupe(troupe: PuppetTroupe) {
        val newPuppets = troupe.puppets.map { puppet ->
            val newStates = puppet.states.map { it.copy() }
            puppet.copy(states = newStates)
        }
        val newSpecialEffectsManager = troupe.specialEffectsManager.copy(
            effects = troupe.specialEffectsManager.effects.map { it.copy() }
        )
        val newTroupe = troupe.copy(puppets = newPuppets, specialEffectsManager = newSpecialEffectsManager)
        _troupe.value = newTroupe
        _activePuppet.value = newTroupe.puppets.find { it.name == newTroupe.activePuppetName }

        val troupeFile = settingsRepository.loadSettings().lastTroupeFile?.let { File(it) } ?: File(System.getProperty("user.dir"), "${newTroupe.name}.troupe")
        saveTroupeAs(troupeFile.absolutePath)

        if (operatingMode == OperatingMode.ONLINE) {
            publishTroupe(settingsRepository.loadSettings().serverIpAddress)
        }
    }

    /**
     * Saves the current troupe to a specific file path.
     * The troupe is saved as a zip file containing the troupe data in a "troupe.json" file and all associated images in an "images" directory.
     * @param filePath The path to save the troupe file to.
     */
    actual fun saveTroupeAs(filePath: String) {
        _troupe.value?.let { troupe ->
            val imageNames = troupe.puppets.flatMap { puppet ->
                puppet.states.flatMap { state ->
                    listOfNotNull(state.imageName, state.blinkImageName) + (state.eyeState?.let {
                        listOfNotNull(
                            it.eyes.left.openState, it.eyes.left.closedState, it.eyes.left.pupil,
                            it.eyes.right.openState, it.eyes.right.closedState, it.eyes.right.pupil
                        )
                    } ?: emptyList())
                }
            }.toSet()

            val file = File(filePath)
            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val troupeJson = json.encodeToString(troupe)
                    val troupeEntry = ZipEntry("troupe.json")
                    zos.putNextEntry(troupeEntry)
                    zos.write(troupeJson.toByteArray())
                    zos.closeEntry()

                    val imagesDirEntry = ZipEntry("images/")
                    zos.putNextEntry(imagesDirEntry)
                    zos.closeEntry()

                    imageNames.forEach { imageName ->
                        val imageFile = File(uploadsDir, imageName)
                        if (imageFile.exists()) {
                            FileInputStream(imageFile).use { fis ->
                                val imageEntry = ZipEntry("images/$imageName")
                                zos.putNextEntry(imageEntry)
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            val settings = settingsRepository.loadSettings().copy(lastTroupeFile = file.absolutePath)
            settingsRepository.saveSettings(settings)
        }
    }

    /**
     * Loads a troupe from a file.
     * The method reads a zip file, extracts the "troupe.json" and images, and updates the current troupe.
     * @param filePath The path to the troupe file.
     * @return The loaded puppet troupe, or null if loading fails.
     */
    actual fun loadTroupeFromFile(filePath: String): PuppetTroupe? {
        val file = File(filePath)
        if (!file.exists()) return null
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                var troupe: PuppetTroupe? = null
                while (entry != null) {
                    when (entry.name) {
                        "troupe.json" -> {
                            troupe = json.decodeFromString<PuppetTroupe>(zis.readBytes().decodeToString())
                        }
                        else -> {
                            if (entry.name.startsWith("images/")) {
                                val imageName = entry.name.substringAfter("images/")
                                if (imageName.isNotEmpty()) {
                                    val imageFile = File(uploadsDir, imageName)
                                    FileOutputStream(imageFile).use {
                                        zis.copyTo(it)
                                    }
                                }
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
                _troupe.value = troupe
                _activePuppet.value = troupe?.puppets?.find { p -> p.name == troupe.activePuppetName }
                val settings = settingsRepository.loadSettings().copy(lastTroupeFile = file.absolutePath)
                settingsRepository.saveSettings(settings)
                return troupe
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * Imports a puppet from a file.
     * The puppet is imported from a ".puppet" zip file and added to the current troupe. If no troupe exists, a new one can be created.
     * @param filePath The path to the puppet file.
     * @param newTroupeName The name for a new troupe if one needs to be created.
     */
    actual fun importPuppet(filePath: String, newTroupeName: String?) {
        val file = File(filePath)
        if (!file.exists() || !file.name.endsWith(".puppet")) return
        try {
            ZipInputStream(FileInputStream(file)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    when (entry.name) {
                        "puppet.json" -> {
                            val puppet = json.decodeFromString<PuppetCharacter>(zis.readBytes().decodeToString())
                            val currentTroupe = _troupe.value
                            if (currentTroupe != null) {
                                val newPuppets = currentTroupe.puppets.filter { p -> p.name != puppet.name } + puppet
                                val newTroupe = currentTroupe.copy(puppets = newPuppets)
                                saveTroupe(newTroupe)
                            } else if (newTroupeName != null) {
                                createNewPuppet(puppet.name, newTroupeName)
                            }
                        }
                        else -> {
                            if (entry.name.startsWith("images/")) {
                                val imageName = entry.name.substringAfter("images/")
                                if (imageName.isNotEmpty()) {
                                    val imageFile = File(uploadsDir, imageName)
                                    FileOutputStream(imageFile).use {
                                        zis.copyTo(it)
                                     }
                                }
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Exports a puppet to a file.
     * The puppet is exported as a ".puppet" zip file containing the puppet data and associated images.
     * @param puppetName The name of the puppet to export.
     * @param exportPath The path to export the puppet file to.
     */
    actual fun exportPuppet(puppetName: String, exportPath: String) {
        _troupe.value?.puppets?.find { it.name == puppetName }?.let { puppet ->
            val imageNames = puppet.states.flatMap { state ->
                listOfNotNull(state.imageName, state.blinkImageName) + (state.eyeState?.let {
                    listOfNotNull(
                        it.eyes.left.openState, it.eyes.left.closedState, it.eyes.left.pupil,
                        it.eyes.right.openState, it.eyes.right.closedState, it.eyes.right.pupil
                    )
                } ?: emptyList())
            }.toSet()

            val file = File(exportPath)
            FileOutputStream(file).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    val puppetJson = json.encodeToString(puppet)
                    val puppetEntry = ZipEntry("puppet.json")
                    zos.putNextEntry(puppetEntry)
                    zos.write(puppetJson.toByteArray())
                    zos.closeEntry()

                    val imagesDirEntry = ZipEntry("images/")
                    zos.putNextEntry(imagesDirEntry)
                    zos.closeEntry()

                    imageNames.forEach { imageName ->
                        val imageFile = File(uploadsDir, imageName)
                        if (imageFile.exists()) {
                            FileInputStream(imageFile).use { fis ->
                                val imageEntry = ZipEntry("images/$imageName")
                                zos.putNextEntry(imageEntry)
                                fis.copyTo(zos)
                                zos.closeEntry()
                            }
                        }
                    }
                }
            }
            val settings = settingsRepository.loadSettings().copy(lastPuppetExportFolder = System.getProperty("user.dir"))
            settingsRepository.saveSettings(settings)
        }
    }

    /**
     * Renames the current troupe.
     * This also deletes the old troupe file.
     * @param newName The new name for the troupe.
     */
    actual fun renameTroupe(newName: String) {
        _troupe.value?.let { currentTroupe ->
            val newTroupe = currentTroupe.copy(name = newName)
            val oldFile = settingsRepository.loadSettings().lastTroupeFile?.let { File(it) }
            if (oldFile?.exists() == true) {
                oldFile.delete()
            }
            saveTroupe(newTroupe)
        }
    }

    /**
     * Creates a new, empty troupe.
     * This clears the current troupe and active puppet.
     */
    actual fun createNewTroupe() {
        _troupe.value = null
        _activePuppet.value = null
        val settings = settingsRepository.loadSettings().copy(lastTroupeFile = null)
        settingsRepository.saveSettings(settings)
    }

    /**
     * Connects to a server and synchronizes the troupe data.
     * If the server has a more up-to-date troupe, it is downloaded. If the server has no troupe, the local troupe is published.
     * @param serverIp The IP address of the server.
     */
    actual fun connectAndSync(serverIp: String) {
        scope.launch {
            _connectionState.value = ConnectionState.CONNECTING
            try {
                val response = client.get("http://$serverIp:${Constants.Server.PORT}/troupe")

                when (response.status) {
                    HttpStatusCode.OK -> {
                        val serverTroupe = response.body<PuppetTroupe>()
                        _connectionState.value = ConnectionState.CONNECTED
                        val localTroupe = _troupe.value
                        val shouldSync = when {
                            localTroupe == null -> true
                            localTroupe.puppets.isEmpty() && serverTroupe.puppets.isNotEmpty() -> true
                            else -> serverTroupe.puppets.any { serverPuppet ->
                                val localPuppet = localTroupe.puppets.find { it.name == serverPuppet.name }
                                localPuppet == null || localPuppet.lastUpdated < serverPuppet.lastUpdated
                            }
                        }

                        if (shouldSync) {
                            serverTroupe.puppets.forEach { puppet ->
                                puppet.states.forEach { state ->
                                    val imageUrl = "http://$serverIp:${Constants.Server.PORT}/uploads/${state.imageName}"
                                    val imageBytes: ByteArray = client.get(imageUrl).body()
                                    File(uploadsDir, state.imageName).writeBytes(imageBytes)

                                    state.blinkImageName?.let { blinkImageName ->
                                        val blinkImageUrl = "http://$serverIp:${Constants.Server.PORT}/uploads/$blinkImageName"
                                        val blinkImageBytes: ByteArray = client.get(blinkImageUrl).body()
                                        File(uploadsDir, blinkImageName).writeBytes(blinkImageBytes)
                                    }

                                    state.eyeState?.let { eyeState ->
                                        suspend fun downloadEyeImage(imageName: String?) {
                                            imageName?.let {
                                                val imageUrl = "http://$serverIp:${Constants.Server.PORT}/uploads/$it"
                                                val imageBytes: ByteArray = client.get(imageUrl).body()
                                                File(uploadsDir, it).writeBytes(imageBytes)
                                            }
                                        }
                                        downloadEyeImage(eyeState.eyes.left.openState)
                                        downloadEyeImage(eyeState.eyes.left.closedState)
                                        downloadEyeImage(eyeState.eyes.left.pupil)
                                        downloadEyeImage(eyeState.eyes.right.openState)
                                        downloadEyeImage(eyeState.eyes.right.closedState)
                                        downloadEyeImage(eyeState.eyes.right.pupil)
                                    }
                                }
                            }

                            _troupe.value = serverTroupe
                            _activePuppet.value = serverTroupe.puppets.find { it.name == serverTroupe.activePuppetName }
                            saveTroupe(serverTroupe)
                        }
                    }
                    HttpStatusCode.NotFound -> {
                        _connectionState.value = ConnectionState.CONNECTED
                        // If we have a local troupe, publish it to the server.
                        _troupe.value?.let {
                            publishTroupe(serverIp)
                        }
                    }
                    else -> {
                        _connectionState.value = ConnectionState.FAILED
                    }
                }
            } catch (e: Exception) {
                println(e)
                _connectionState.value = ConnectionState.FAILED
            }
        }
    }


    /**
     * Sets the active puppet.
     * @param name The name of the puppet to set as active.
     */
    actual fun setActivePuppet(name: String) {
        _troupe.value?.let { currentTroupe ->
            val newTroupe = currentTroupe.copy(activePuppetName = name)
            _troupe.value = newTroupe
            _activePuppet.value = newTroupe.puppets.find { it.name == name }
            saveTroupe(newTroupe)
        }
    }

    /**
     * Creates a new puppet.
     * If a troupe exists, the puppet is added to it. Otherwise, a new troupe is created.
     * @param name The name of the new puppet.
     * @param troupeName The name of the new troupe, if one needs to be created.
     */
    actual fun createNewPuppet(name: String, troupeName: String?) {
        if (_troupe.value?.puppets?.any { it.name == name } == true) return

        val newPuppet = PuppetCharacter(name, System.currentTimeMillis(), emptyList())
        val currentTroupe = _troupe.value

        val newTroupe = if (currentTroupe == null) {
            val troupe = PuppetTroupe(troupeName!!, name, listOf(newPuppet), SpecialEffectsManager())
            val troupeFile = File(System.getProperty("user.dir"), "$troupeName.troupe")
            saveTroupeAs(troupeFile.absolutePath)
            troupe
        } else {
            currentTroupe.copy(
                puppets = currentTroupe.puppets + newPuppet,
                activePuppetName = name
            )
        }

        _troupe.value = newTroupe
        _activePuppet.value = newPuppet
        saveTroupe(newTroupe)
    }

    actual fun renamePuppet(newName: String) {
        if (_troupe.value?.puppets?.any { it.name == newName } == true) return

        _activePuppet.value?.let { activePuppet ->
            _troupe.value?.let { currentTroupe ->
                val oldName = activePuppet.name
                val updatedPuppet = activePuppet.copy(name = newName, lastUpdated = System.currentTimeMillis())

                val newPuppets = currentTroupe.puppets.map {
                    if (it.name == oldName) updatedPuppet else it
                }

                val newTroupe = currentTroupe.copy(
                    puppets = newPuppets,
                    activePuppetName = newName
                )

                saveTroupe(newTroupe)
            }
        }
    }

    /**
     * Creates a new state for the active puppet.
     * @param stateName The name of the new state.
     * @param imageBytes The image data for the state.
     * @param localImageName The local name of the image.
     * @param blinkImageBytes The image data for the blink state (optional).
     * @param localBlinkImageName The local name of the blink image (optional).
     * @param serverIp The IP address of the server, used for online mode.
     */
    actual fun createNewState(
        stateName: String,
        imageBytes: ByteArray,
        localImageName: String,
        blinkImageBytes: ByteArray?,
        localBlinkImageName: String?,
        serverIp: String
    ) {
        scope.launch {
            val serverImageName = if (operatingMode == OperatingMode.ONLINE) uploader.upload(imageBytes, localImageName, serverIp) else localImageName
            saveImage(serverImageName, imageBytes)

            var serverBlinkImageName: String? = null
            if (blinkImageBytes != null && localBlinkImageName != null) {
                val name = if (operatingMode == OperatingMode.ONLINE) uploader.upload(blinkImageBytes, localBlinkImageName, serverIp) else localBlinkImageName
                saveImage(name, blinkImageBytes)
                serverBlinkImageName = name
            }

            _activePuppet.value?.let { currentPuppet ->
                val existingState = currentPuppet.states.find { it.name == stateName }

                val newState = if (existingState != null) {
                    existingState.copy(
                        imageName = serverImageName,
                        blinkImageName = serverBlinkImageName ?: existingState.blinkImageName
                    )
                } else {
                    PuppetStateInfo(
                        name = stateName,
                        imageName = serverImageName,
                        blinkImageName = serverBlinkImageName
                    )
                }

                val newStates = currentPuppet.states.filter { it.name != stateName } + newState
                updatePuppet(currentPuppet.name) { it.copy(states = newStates) }
            }
        }
    }

    /**
     * Updates a puppet with a given transformation function.
     * @param puppetName The name of the puppet to update.
     * @param update A function that takes the current puppet and returns the updated puppet.
     */
    actual fun updatePuppet(puppetName: String, update: (PuppetCharacter) -> PuppetCharacter) {
        _troupe.value?.let { troupe ->
            val newPuppets = troupe.puppets.map { if (it.name == puppetName) update(it).copy(lastUpdated = System.currentTimeMillis()) else it }
            val newTroupe = troupe.copy(puppets = newPuppets)
            saveTroupe(newTroupe)
        }
    }

    /**
     * Saves an image to the uploads directory.
     * If in online mode, it also uploads the image to the server.
     * @param name The name of the image.
     * @param data The image data.
     */
    actual fun saveImage(name: String, data: ByteArray) {
        File(uploadsDir, name).writeBytes(data)
        if (operatingMode == OperatingMode.ONLINE) {
            scope.launch {
                uploader.upload(data, name, SettingsRepository(context).loadSettings().serverIpAddress)
            }
        }
    }

    /**
     * Publishes the current troupe to the server.
     * This uploads all associated images and then sends the troupe data to the server.
     * @param serverIp The IP address of the server.
     */
    actual fun publishTroupe(serverIp: String) {
        scope.launch {
            _troupe.value?.let { troupe ->
                troupe.puppets.forEach { puppet ->
                    puppet.states.forEach { state ->
                        File(uploadsDir, state.imageName).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), state.imageName, serverIp) }
                        state.blinkImageName?.let { blinkName ->
                            File(uploadsDir, blinkName).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), blinkName, serverIp) }
                        }
                        state.eyeState?.let { eyeState ->
                            val leftEye = eyeState.eyes.left
                            val rightEye = eyeState.eyes.right

                            File(uploadsDir, leftEye.openState).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), leftEye.openState, serverIp) }
                            leftEye.closedState?.let { closedState ->
                                File(uploadsDir, closedState).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), closedState, serverIp) }
                            }
                            leftEye.pupil?.let { pupil ->
                                File(uploadsDir, pupil).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), pupil, serverIp) }
                            }

                            File(uploadsDir, rightEye.openState).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), rightEye.openState, serverIp) }
                            rightEye.closedState?.let { closedState ->
                                File(uploadsDir, closedState).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), closedState, serverIp) }
                            }
                            rightEye.pupil?.let { pupil ->
                                File(uploadsDir, pupil).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), pupil, serverIp) }
                            }
                        }
                    }
                }
                client.post("http://$serverIp:${Constants.Server.PORT}/troupe") { contentType(ContentType.Application.Json); setBody(troupe) }
            }
        }
    }

    actual fun updateNodeGraph(nodeGraph: NodeGraph) {
        _troupe.value?.let {
            saveTroupe(it.copy(nodeGraph = nodeGraph))
        }
    }
}
