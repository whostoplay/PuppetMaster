package org.menagerie.puppet_master

import android.content.Context
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

actual class PuppetDataManager actual constructor(private val scope: CoroutineScope, private val context: Any) {

    actual val uploadsDir = getUploadsDir(context)
    private val uploader = Uploader()
    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true; encodeDefaults = true }) }
    }
    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true; encodeDefaults = true }
    private val localTroupeFile = File(uploadsDir, "local_troupe.json")

    private val _troupe = MutableStateFlow<PuppetTroupe?>(null)
    actual val troupe: StateFlow<PuppetTroupe?> = _troupe

    private val _activePuppet = MutableStateFlow<PuppetCharacter?>(null)
    actual val activePuppet: StateFlow<PuppetCharacter?> = _activePuppet

    private var operatingMode: OperatingMode = OperatingMode.OFFLINE

    init {
        scope.launch {
            val localTroupe = loadLocalTroupe()
            if (localTroupe != null) {
                _troupe.value = localTroupe
                _activePuppet.value = localTroupe.puppets.find { it.name == localTroupe.activePuppetName }
            }
        }
    }

    actual suspend fun getImageData(imageName: String): ByteArray? {
        val file = File(uploadsDir, imageName)
        return if (file.exists()) file.readBytes() else null
    }

    actual fun setOperatingMode(mode: OperatingMode) {
        operatingMode = mode
    }

    actual fun saveTroupe(troupe: PuppetTroupe) {
        // Create a new list of new puppet/state objects to ensure StateFlow emits the update
        val newPuppets = troupe.puppets.map { puppet ->
            val newStates = puppet.states.map { state -> state.copy() }
            puppet.copy(states = newStates)
        }
        val newSpecialEffectsManager = troupe.specialEffectsManager.copy(
            effects = troupe.specialEffectsManager.effects.map { it.copy() }
        )
        val newTroupe = troupe.copy(puppets = newPuppets, specialEffectsManager = newSpecialEffectsManager)
        _troupe.value = newTroupe
        _activePuppet.value = newTroupe.puppets.find { it.name == newTroupe.activePuppetName }
        saveLocalTroupe(newTroupe)
        if (operatingMode == OperatingMode.ONLINE) {
            publishTroupe(SettingsRepository(context as Context).loadIp())
        }
    }

    actual fun connectAndSync(serverIp: String) {
        scope.launch {
            try {
                val serverTroupe = client.get("http://$serverIp:$SERVER_PORT/troupe").body<PuppetTroupe>()
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
                            val imageUrl = "http://$serverIp:$SERVER_PORT/uploads/${state.imageName}"
                            val imageBytes: ByteArray = client.get(imageUrl).body()
                            File(uploadsDir, state.imageName).writeBytes(imageBytes)

                            state.blinkImageName?.let { blinkImageName ->
                                val blinkImageUrl = "http://$serverIp:$SERVER_PORT/uploads/$blinkImageName"
                                val blinkImageBytes: ByteArray = client.get(blinkImageUrl).body()
                                File(uploadsDir, blinkImageName).writeBytes(blinkImageBytes)
                            }
                        }
                    }

                    _troupe.value = serverTroupe
                    _activePuppet.value = serverTroupe.puppets.find { it.name == serverTroupe.activePuppetName }
                    saveLocalTroupe(serverTroupe)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    actual fun setActivePuppet(name: String) {
        _troupe.value?.let { currentTroupe ->
            val newTroupe = currentTroupe.copy(activePuppetName = name)
            _troupe.value = newTroupe
            _activePuppet.value = newTroupe.puppets.find { it.name == name }
            saveLocalTroupe(newTroupe)
        }
    }

    actual fun createNewPuppet(name: String) {
        if (_troupe.value?.puppets?.any { it.name == name } == true) return

        val newPuppet = PuppetCharacter(name, System.currentTimeMillis(), emptyList())
        val currentTroupe = _troupe.value ?: PuppetTroupe(activePuppetName = "", puppets = emptyList(), specialEffectsManager = SpecialEffectsManager())

        val newTroupe = currentTroupe.copy(
            puppets = currentTroupe.puppets + newPuppet,
            activePuppetName = name
        )

        _troupe.value = newTroupe
        _activePuppet.value = newPuppet
        saveLocalTroupe(newTroupe)
    }

    actual fun createNewState(
        stateName: String, imageBytes: ByteArray, localImageName: String,
        blinkImageBytes: ByteArray?, localBlinkImageName: String?,
        serverIp: String
    ) {
        scope.launch {
            val serverImageName = if (operatingMode == OperatingMode.ONLINE) uploader.upload(imageBytes, localImageName, serverIp) else localImageName
            File(uploadsDir, serverImageName).writeBytes(imageBytes)

            var serverBlinkImageName: String? = null
            if (blinkImageBytes != null && localBlinkImageName != null) {
                serverBlinkImageName = if (operatingMode == OperatingMode.ONLINE) uploader.upload(blinkImageBytes, localBlinkImageName, serverIp) else localBlinkImageName
                File(uploadsDir, serverBlinkImageName).writeBytes(blinkImageBytes)
            }

            val newState = PuppetStateInfo(name = stateName, imageName = serverImageName, blinkImageName = serverBlinkImageName)

            _activePuppet.value?.let { currentPuppet ->
                val otherStates = currentPuppet.states.filter { it.name != stateName }
                val newStates = otherStates + newState
                updatePuppet(currentPuppet.name) { it.copy(states = newStates) }
            }
        }
    }

    actual fun updatePuppet(puppetName: String, update: (PuppetCharacter) -> PuppetCharacter) {
        _troupe.value?.let { troupe ->
            val newPuppets = troupe.puppets.map { if (it.name == puppetName) update(it).copy(lastUpdated = System.currentTimeMillis()) else it }
            val newTroupe = troupe.copy(puppets = newPuppets)
            _troupe.value = newTroupe
            _activePuppet.value = newPuppets.find { it.name == puppetName }
            saveLocalTroupe(newTroupe)
        }
    }

    private fun loadLocalTroupe(): PuppetTroupe? = try {
        if (!localTroupeFile.exists()) null
        else json.decodeFromString(localTroupeFile.readText())
    } catch (e: Exception) { null }

    private fun saveLocalTroupe(troupe: PuppetTroupe) {
        localTroupeFile.writeText(json.encodeToString(troupe))
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
                client.post("http://$serverIp:$SERVER_PORT/troupe") { contentType(ContentType.Application.Json); setBody(troupe) }
            }
        }
    }
}
