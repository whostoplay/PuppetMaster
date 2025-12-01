package org.menagerie.puppet_master

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.math.pow
import kotlin.random.Random

enum class OperatingMode {
    ONLINE, OFFLINE
}

class MainViewModel(context: Any) : ViewModel() {

    val uploadsDir = getUploadsDir(context)
    private val uploader = Uploader()
    private val audioProcessor = AudioProcessor(context)
    private val client = HttpClient {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
        install(WebSockets)
    }
    private val gson = Gson()
    private val localTroupeFile = File(uploadsDir, "local_troupe.json")

    private val _troupe = MutableStateFlow<Troupe?>(null)
    val troupe: StateFlow<Troupe?> = _troupe

    private val _activePuppet = MutableStateFlow<Puppet?>(null)
    val activePuppet: StateFlow<Puppet?> = _activePuppet

    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    val activeState: StateFlow<PuppetStateInfo?> = _activeState

    private val _selectedState = MutableStateFlow<PuppetStateInfo?>(null)
    val selectedState: StateFlow<PuppetStateInfo?> = _selectedState

    private val _displayedImageName = MutableStateFlow<String?>(null)
    val displayedImageName: StateFlow<String?> = _displayedImageName

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing

    private val _operatingMode = MutableStateFlow(OperatingMode.OFFLINE)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _thresholds = MutableStateFlow<Map<Float, PuppetStateInfo?>>(emptyMap())
    val thresholds: StateFlow<Map<Float, PuppetStateInfo?>> = _thresholds.asStateFlow()

    private var serverStateJob: Job? = null
    private var clientControlSocketJob: Job? = null
    private var clientControlSocket: ClientWebSocketSession? = null
    private var clientBlinkingJob: Job? = null
    private var returnToIdleJob: Job? = null

    init {
        smartLoad()

        viewModelScope.launch {
            displayedImageName.collect { imageName ->
                if (_isPublishing.value && clientControlSocket != null && imageName != null) {
                    try {
                        clientControlSocket?.send(imageName)
                    } catch (e: Exception) {
                        println("Failed to publish state: ${e.message}")
                    }
                }
            }
        }

        viewModelScope.launch {
            activeState.collect { state ->
                clientBlinkingJob?.cancel()
                _displayedImageName.value = state?.imageName

                if (state?.blinkImageName != null) {
                    clientBlinkingJob = launch {
                        while (true) {
                            val delayTime = if (state.minBlinkRate >= state.maxBlinkRate) {
                                state.maxBlinkRate
                            } else {
                                Random.nextLong(state.minBlinkRate, state.maxBlinkRate)
                            }
                            delay(delayTime)

                            val isClientInControl = _operatingMode.value == OperatingMode.OFFLINE || _isPublishing.value

                            if (isClientInControl && activeState.value == state) {
                                _displayedImageName.value = state.blinkImageName
                                delay(150)
                                _displayedImageName.value = state.imageName
                            }
                        }
                    }
                }
            }
        }
    }

    fun setOperatingMode(mode: OperatingMode) {
        _operatingMode.value = mode
        if (mode == OperatingMode.ONLINE) {
            if (_isPublishing.value) {
                setPublishing(false)
            }
            observeServerState()
        } else { // OFFLINE
            serverStateJob?.cancel()
            _activeState.value = _activePuppet.value?.states?.find { it.name == "idle" }
        }
    }

    fun setActivePuppet(name: String) {
        _troupe.value?.let { currentTroupe ->
            val newTroupe = currentTroupe.copy(activePuppetName = name)
            _troupe.value = newTroupe
            val newActivePuppet = newTroupe.puppets.find { it.name == name }
            _activePuppet.value = newActivePuppet
            _activeState.value = newActivePuppet?.states?.find { it.name == "idle" }

            // Load thresholds from the newly activated puppet
            _thresholds.value = newActivePuppet?.thresholds ?: emptyMap()

            saveLocalTroupe(newTroupe)
        }
    }

    fun createNewPuppet(name: String) {
        // Prevent creating puppet with duplicate name
        if (_troupe.value?.puppets?.any { it.name == name } == true) {
            return
        }

        val newPuppet = Puppet(name, System.currentTimeMillis(), emptyList())
        val currentTroupe = _troupe.value ?: Troupe(activePuppetName = "", puppets = emptyList())

        val newTroupe = currentTroupe.copy(
            puppets = currentTroupe.puppets + newPuppet,
            activePuppetName = name
        )

        _troupe.value = newTroupe
        _activePuppet.value = newPuppet
        _activeState.value = null // A new puppet starts with no states
        _thresholds.value = emptyMap() // Clear thresholds for new puppet
        saveLocalTroupe(newTroupe)
    }

    fun setPublishing(isPublishing: Boolean) {
        if (_operatingMode.value == OperatingMode.OFFLINE) return

        _isPublishing.value = isPublishing

        if (isPublishing) {
            viewModelScope.launch {
                _troupe.value?.let { client.post("http://127.0.0.1:$SERVER_PORT/troupe") { contentType(ContentType.Application.Json); setBody(it) } }
            }
            serverStateJob?.cancel()
            startClientControl()
        } else {
            stopClientControl()
            observeServerState()
        }
    }

    fun selectState(state: PuppetStateInfo) {
        _selectedState.value = state
    }

    fun updateBlinkRate(state: PuppetStateInfo, blinkRate: LongRange) {
        updatePuppetState(state.name) { it.copy(minBlinkRate = blinkRate.first, maxBlinkRate = blinkRate.last) }
    }

    private fun smartLoad() {
        viewModelScope.launch {
            val localTroupe = loadLocalTroupe()
            if (localTroupe != null) {
                _troupe.value = localTroupe
                val activePuppet = localTroupe.puppets.find { it.name == localTroupe.activePuppetName }
                _activePuppet.value = activePuppet
                _activeState.value = activePuppet?.states?.find { it.name == "idle" }
                _thresholds.value = activePuppet?.thresholds ?: emptyMap()
            }

            try {
                val serverTroupe = client.get("http://127.0.0.1:$SERVER_PORT/troupe").body<Troupe>()
                if (localTroupe == null || serverTroupe.puppets.any { sp -> localTroupe.puppets.find { lp -> lp.name == sp.name }?.lastUpdated ?: 0 < sp.lastUpdated }) {
                    _troupe.value = serverTroupe
                    val activePuppet = serverTroupe.puppets.find { it.name == serverTroupe.activePuppetName }
                    _activePuppet.value = activePuppet
                    _activeState.value = activePuppet?.states?.find { it.name == "idle" }
                    _thresholds.value = activePuppet?.thresholds ?: emptyMap()
                    saveLocalTroupe(serverTroupe)
                }
            } catch (e: Exception) {
                // Could not reach server, remain in offline mode
                if (localTroupe == null) {
                    _troupe.value = null
                    _activePuppet.value = null
                    _activeState.value = null
                }
            }
        }
    }

    private fun loadLocalTroupe(): Troupe? = try {
        if (!localTroupeFile.exists()) null
        else gson.fromJson(localTroupeFile.readText(), Troupe::class.java)
    } catch (e: Exception) { null }

    private fun saveLocalTroupe(troupe: Troupe) {
        localTroupeFile.writeText(gson.toJson(troupe))
    }

    fun createNewState(stateName: String, imageBytes: ByteArray, localImageName: String, blinkImageBytes: ByteArray?, localBlinkImageName: String?) {
        viewModelScope.launch {
            val serverImageName = if (_operatingMode.value == OperatingMode.ONLINE) {
                uploader.upload(imageBytes, localImageName)
            } else {
                localImageName
            }
            val localFile = File(uploadsDir, serverImageName)
            localFile.writeBytes(imageBytes)

            var serverBlinkImageName: String? = null
            if (blinkImageBytes != null && localBlinkImageName != null) {
                serverBlinkImageName = if (_operatingMode.value == OperatingMode.ONLINE) {
                    uploader.upload(blinkImageBytes, localBlinkImageName)
                } else {
                    localBlinkImageName
                }
                val localBlinkFile = File(uploadsDir, serverBlinkImageName)
                localBlinkFile.writeBytes(blinkImageBytes)
            }

            val newState = PuppetStateInfo(name = stateName, imageName = serverImageName, blinkImageName = serverBlinkImageName)

            _activePuppet.value?.let { currentPuppet ->
                val otherStates = currentPuppet.states.orEmpty().filter { it.name != stateName }
                val newStates = otherStates + newState
                updatePuppet(currentPuppet.name) { it.copy(states = newStates, lastUpdated = System.currentTimeMillis()) }
            }
        }
    }

    fun toggleListening() {
        val newListeningState = !_isListening.value
        if (newListeningState) {
            startListening()
        } else {
            stopListening()
        }
    }

    fun addThreshold(value: Float) {
        val newThresholds = _thresholds.value.toMutableMap()
        newThresholds[value] = null
        _thresholds.value = newThresholds
        persistThresholds()
    }

    fun updateThreshold(oldValue: Float, newValue: Float) {
        val newThresholds = _thresholds.value.toMutableMap()
        val state = newThresholds.remove(oldValue)
        newThresholds[newValue] = state
        _thresholds.value = newThresholds
        persistThresholds()
    }

    fun assignStateToThreshold(value: Float, state: PuppetStateInfo) {
        val newThresholds = _thresholds.value.toMutableMap()
        newThresholds[value] = state
        _thresholds.value = newThresholds
        persistThresholds()
    }

    private fun persistThresholds() {
        _activePuppet.value?.let { puppet ->
            val thresholdsToSave = _thresholds.value.filterValues { it != null }.mapValues { it.value!! }
            updatePuppet(puppet.name) { it.copy(thresholds = thresholdsToSave, lastUpdated = System.currentTimeMillis()) }
        }
    }

    private fun updatePuppet(puppetName: String, update: (Puppet) -> Puppet) {
        _troupe.value?.let { troupe ->
            val newPuppets = troupe.puppets.map {
                if (it.name == puppetName) update(it) else it
            }
            val newTroupe = troupe.copy(puppets = newPuppets)
            _troupe.value = newTroupe
            _activePuppet.value = newPuppets.find { it.name == puppetName }
            saveLocalTroupe(newTroupe)
        }
    }

    private fun updatePuppetState(stateName: String, update: (PuppetStateInfo) -> PuppetStateInfo) {
        _activePuppet.value?.let { puppet ->
            val newStates = puppet.states.map {
                if (it.name == stateName) update(it) else it
            }
            updatePuppet(puppet.name) { it.copy(states = newStates) }
        }
    }

    private fun startListening() {
        _isListening.value = true
        audioProcessor.start { level ->
            _audioLevel.value = level
            val isControlling = _operatingMode.value == OperatingMode.OFFLINE || _isPublishing.value

            if (isControlling) {
                val scaledLevel = level.pow(0.5f)
                val sortedThresholds = _thresholds.value.entries.sortedBy { it.key }
                val activeThresholdIndex = sortedThresholds.indexOfLast { scaledLevel >= it.key }

                if (activeThresholdIndex != -1) {
                    returnToIdleJob?.cancel()
                    var state: PuppetStateInfo? = null
                    for (i in activeThresholdIndex downTo 0) {
                        if (sortedThresholds[i].value != null) {
                            state = sortedThresholds[i].value
                            break
                        }
                    }
                    if (state != null) {
                        _activeState.value = state
                    } else {
                        if (_activeState.value?.name != "idle") {
                            returnToIdleJob = viewModelScope.launch {
                                delay(100)
                                _activeState.value = _activePuppet.value?.states?.find { it.name == "idle" }
                            }
                        }
                    }
                } else {
                    if (_activeState.value?.name != "idle") {
                        returnToIdleJob?.cancel()
                        returnToIdleJob = viewModelScope.launch {
                            delay(100)
                            _activeState.value = _activePuppet.value?.states?.find { it.name == "idle" }
                        }
                    }
                }
            }
        }
    }

    private fun stopListening() {
        _isListening.value = false
        _audioLevel.value = 0f
        audioProcessor.stop()
    }

    private fun observeServerState() {
        clientBlinkingJob?.cancel() // When observing server, server is the source of truth for blinks.
        serverStateJob = viewModelScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = SERVER_PORT, path = "/obs") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val imageUrl = frame.readText()
                            val imageName = imageUrl.substringAfterLast("/")
                            _displayedImageName.value = imageName

                            val newActiveState = _activePuppet.value?.states?.find { it.imageName == imageName || it.blinkImageName == imageName }
                            if (newActiveState != null && _activeState.value != newActiveState) {
                                _activeState.value = newActiveState
                            }
                        }
                    }
                }
            } catch (e: Exception) { /* Handle error */ }
        }
    }

    private fun startClientControl() {
        clientControlSocketJob = viewModelScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = SERVER_PORT, path = "/client-control") {
                    clientControlSocket = this
                    // Resend current state upon connection
                    _displayedImageName.value?.let {
                        send(it)
                    }
                    // Suspend to keep the socket open
                    incoming.receive()
                }
            } catch (e: Exception) {
                println("Client control socket error: ${e.message}")
            } finally {
                clientControlSocket = null
            }
        }
    }

    private fun stopClientControl() {
        viewModelScope.launch {
            clientControlSocket?.close()
            clientControlSocketJob?.cancel()
            clientControlSocket = null
        }
    }
}
