package org.menagerie.puppet_master

import android.content.Context
import androidx.core.net.toUri
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
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
 * Android-specific implementation of the [PuppetDataManager].
 *
 * This class manages puppet and troupe data on Android, handling file operations, network synchronization,
 * and state management. It uses Android's [Context] for file system access and content resolution.
 *
 * @param scope The [CoroutineScope] for launching background tasks.
 * @param context The Android application [Context].
 */
@OptIn(ExperimentalResourceApi::class)
actual class PuppetDataManager actual constructor(private val scope: CoroutineScope, private val context: Any) {

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
    actual val troupe: StateFlow<PuppetTroupe?> = _troupe

    private val _activePuppet = MutableStateFlow<PuppetCharacter?>(null)
    actual val activePuppet: StateFlow<PuppetCharacter?> = _activePuppet

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    actual val connectionState: StateFlow<ConnectionState> = _connectionState

    private var operatingMode: OperatingMode = OperatingMode.OFFLINE

    init {
        scope.launch {
            reloadLastTroupe()
        }
    }

    /**
     * Reloads the last used troupe from storage.
     *
     * It first checks the saved settings for a file path. If not found, it searches the app's
     * internal directory for the most recently modified `.troupe` file. If no troupe is found,
     * a default one is created.
     */
    actual fun reloadLastTroupe() {
        val settings = settingsRepository.loadSettings()
        val troupeFile = settings.lastTroupeFile?.let { File(it) }

        val localTroupe = if (troupeFile?.exists() == true) {
            loadTroupeFromFile(troupeFile.absolutePath)
        } else {
            val mostRecentTroupe = (context as Context).filesDir.listFiles { _, name -> name.endsWith(".troupe") }?.maxByOrNull { it.lastModified() }
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

    private fun createDefaultTroupe(): PuppetTroupe {
        scope.launch {
            extractDefaultImages()
        }

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
     * Retrieves image data for a given image name.
     *
     * This function can handle both `content://` URIs from the Android MediaStore and local
     * filenames from the app's internal `uploads` directory.
     *
     * @param imageName The URI or filename of the image.
     * @return A [ByteArray] of the image data, or null if not found.
     */
    actual suspend fun getImageData(imageName: String): ByteArray? {
        val androidContext = context as Context
        return try {
            val uri = imageName.toUri()
            if (uri.scheme == "content") {
                androidContext.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } else {
                val file = File(uploadsDir, imageName)
                if (file.exists()) file.readBytes() else null
            }
        } catch (_: Exception) {
            val file = File(uploadsDir, imageName)
            if (file.exists()) file.readBytes() else null
        }
    }

    actual fun setOperatingMode(mode: OperatingMode) {
        operatingMode = mode
    }

    /**
     * Saves the current troupe state and persists it.
     * In online mode, this also triggers a publish to the server.
     *
     * @param troupe The [PuppetTroupe] to save.
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

        val troupeFile = settingsRepository.loadSettings().lastTroupeFile?.let { File(it) } ?: File((context as Context).filesDir, "${newTroupe.name}.troupe")
        saveTroupeAs(troupeFile.absolutePath)

        if (operatingMode == OperatingMode.ONLINE) {
            publishTroupe(settingsRepository.loadSettings().serverIpAddress)
        }
    }

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
            val settings = settingsRepository.loadSettings().copy(lastPuppetExportFolder = (context as Context).filesDir.absolutePath)
            settingsRepository.saveSettings(settings)
        }
    }

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
                _connectionState.value = ConnectionState.FAILED
            }
        }
    }

    actual fun setActivePuppet(name: String) {
        _troupe.value?.let { currentTroupe ->
            val newTroupe = currentTroupe.copy(activePuppetName = name)
            _troupe.value = newTroupe
            _activePuppet.value = newTroupe.puppets.find { it.name == name }
            saveTroupe(newTroupe)
        }
    }

    actual fun createNewPuppet(name: String, troupeName: String?) {
        if (_troupe.value?.puppets?.any { it.name == name } == true) return

        val newPuppet = PuppetCharacter(name, System.currentTimeMillis(), emptyList())
        val currentTroupe = _troupe.value

        val newTroupe = if (currentTroupe == null) {
            val troupe = PuppetTroupe(troupeName!!, name, listOf(newPuppet), SpecialEffectsManager())
            val troupeFile = File((context as Context).filesDir, "$troupeName.troupe")
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

    actual fun createNewTroupe() {
        _troupe.value = null
        _activePuppet.value = null
        val settings = settingsRepository.loadSettings().copy(lastTroupeFile = null)
        settingsRepository.saveSettings(settings)
    }

    actual fun createNewState(
        stateName: String, imageBytes: ByteArray, localImageName: String,
        blinkImageBytes: ByteArray?, localBlinkImageName: String?,
        serverIp: String
    ) {
        scope.launch {
            val serverImageName = if (operatingMode == OperatingMode.ONLINE) uploader.upload(imageBytes, localImageName, serverIp) else localImageName
            saveImage(serverImageName, imageBytes)

            var serverBlinkImageName: String? = null
            if (blinkImageBytes != null && localBlinkImageName != null) {
                serverBlinkImageName = if (operatingMode == OperatingMode.ONLINE) uploader.upload(blinkImageBytes, localBlinkImageName, serverIp) else localBlinkImageName
                saveImage(serverBlinkImageName, blinkImageBytes)
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

    actual fun updatePuppet(puppetName: String, update: (PuppetCharacter) -> PuppetCharacter) {
        _troupe.value?.let { troupe ->
            val newPuppets = troupe.puppets.map { if (it.name == puppetName) update(it).copy(lastUpdated = System.currentTimeMillis()) else it }
            val newTroupe = troupe.copy(puppets = newPuppets)
            saveTroupe(newTroupe)
        }
    }

    actual fun publishTroupe(serverIp: String) {
        scope.launch {
            _troupe.value?.let { troupe ->
                troupe.puppets.forEach { puppet ->
                    puppet.states.forEach { state ->
                        File(uploadsDir, state.imageName).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), state.imageName, serverIp) }
                        state.blinkImageName?.let { blinkName ->
                            File(uploadsDir, blinkName).takeIf { it.exists() }?.let { uploader.upload(it.readBytes(), blinkName, serverIp) }
                        }
                    }
                }
                client.post("http://$serverIp:${Constants.Server.PORT}/troupe") { contentType(ContentType.Application.Json); setBody(troupe) }
            }
        }
    }

    actual fun saveImage(name: String, data: ByteArray) {
        val file = File(uploadsDir, name)
        FileOutputStream(file).use { it.write(data) }
        if (operatingMode == OperatingMode.ONLINE) {
            scope.launch {
                uploader.upload(data, name, SettingsRepository(context as Context).loadSettings().serverIpAddress)
            }
        }
    }

    actual fun updateNodeGraph(nodeGraph: NodeGraph) {
        _troupe.value?.let {
            saveTroupe(it.copy(nodeGraph = nodeGraph))
        }
    }
}
