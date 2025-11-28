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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.File

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
    private val localConfigFile = File(uploadsDir, "local_puppet_config.json")

    private val _localPuppetConfig = MutableStateFlow<PuppetConfiguration?>(null)
    val localPuppetConfig: StateFlow<PuppetConfiguration?> = _localPuppetConfig

    private val _operatingMode = MutableStateFlow(OperatingMode.OFFLINE)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode

    private val _activeState = MutableStateFlow<PuppetStateInfo?>(null)
    val activeState: StateFlow<PuppetStateInfo?> = _activeState

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private var serverStateJob: Job? = null
    private var clientControlSocketJob: Job? = null
    private var clientControlSocket: ClientWebSocketSession? = null

    init {
        smartLoad()
        // This collector will handle publishing the state to the server
        viewModelScope.launch {
            activeState.collect { state ->
                if (clientControlSocket != null && state != null) {
                    try {
                        clientControlSocket?.send(Json.encodeToString(state))
                    } catch (e: Exception) {
                        println("Failed to publish state: ${e.message}")
                    }
                }
            }
        }
    }

    fun setOperatingMode(mode: OperatingMode) {
        _operatingMode.value = mode
        if (mode == OperatingMode.ONLINE) {
            // When going online, default to observing the server, not publishing.
            if (_isPublishing.value) {
                togglePublishing() // This will also handle stopping the control socket
            }
            observeServerState()
        } else { // OFFLINE
            serverStateJob?.cancel()
            if (_isPublishing.value) {
                togglePublishing()
            }
            // In offline mode, default to idle
            _activeState.value = _localPuppetConfig.value?.states?.find { it.name == "idle" }
        }
    }

    fun togglePublishing() {
        if (_operatingMode.value == OperatingMode.OFFLINE) return // Can't publish in offline mode

        val newPublishingState = !_isPublishing.value
        _isPublishing.value = newPublishingState

        if (newPublishingState) {
            // Start publishing, so stop observing
            serverStateJob?.cancel()
            startClientControl() 
        } else {
            // Stop publishing, so start observing
            stopClientControl()
            observeServerState()
        }
    }

    private fun smartLoad() {
        viewModelScope.launch {
            val localConfig = loadLocalConfig()
            _localPuppetConfig.value = localConfig
            _activeState.value = localConfig?.states?.find { it.name == "idle" }
            
            try {
                val serverConfig = client.get("http://127.0.0.1:$SERVER_PORT/config").body<PuppetConfiguration>()
                if (localConfig == null || serverConfig.lastUpdated > localConfig.lastUpdated) {
                    _localPuppetConfig.value = serverConfig
                    saveLocalConfig(serverConfig)
                    _activeState.value = serverConfig.states.find { it.name == "idle" }
                }
            } catch (e: Exception) {
                // Could not reach server, remain in offline mode
            }
        }
    }

    private fun loadLocalConfig(): PuppetConfiguration? = try {
        if (!localConfigFile.exists()) null
        else gson.fromJson(localConfigFile.readText(), PuppetConfiguration::class.java)
    } catch (e: Exception) { null }

    private fun saveLocalConfig(config: PuppetConfiguration) {
        localConfigFile.writeText(gson.toJson(config))
    }

    fun publishConfiguration() {
        viewModelScope.launch {
            _localPuppetConfig.value?.let { client.post("http://127.0.0.1:$SERVER_PORT/config") { contentType(ContentType.Application.Json); setBody(it) } }
        }
    }

    fun createNewState(stateName: String, imageBytes: ByteArray, localImageName: String) {
        viewModelScope.launch {
            val serverImageName = uploader.upload(imageBytes, localImageName)
            
            // Save the image locally for offline use
            val localFile = File(uploadsDir, serverImageName)
            localFile.writeBytes(imageBytes)

            val newState = PuppetStateInfo(name = stateName, imageName = serverImageName)
            val currentConfig = _localPuppetConfig.value
            val newStates = currentConfig?.states.orEmpty() + newState
            val newConfig = PuppetConfiguration(System.currentTimeMillis(), newStates)
            _localPuppetConfig.value = newConfig
            saveLocalConfig(newConfig)

            if (_activeState.value == null) {
                _activeState.value = newState
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

    private fun startListening() {
        _isListening.value = true
        audioProcessor.start { isSpeaking ->
            val isControlling = (_operatingMode.value == OperatingMode.OFFLINE) ||
                                (_operatingMode.value == OperatingMode.ONLINE && _isPublishing.value)

            if (isControlling) {
                // Update local state directly. If publishing, the collector will send it to the server.
                val targetStateName = if (isSpeaking) "talking" else "idle"
                _activeState.value = _localPuppetConfig.value?.states?.find { it.name == targetStateName }
            }
            // If ONLINE and not PUBLISHING, we are in viewer mode, so local audio input does nothing.
        }
    }
    
    private fun stopListening() {
        _isListening.value = false
        audioProcessor.stop()
    }

    private fun observeServerState() {
        serverStateJob = viewModelScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = "127.0.0.1", port = SERVER_PORT, path = "/obs") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val imageUrl = frame.readText()
                            val imageName = imageUrl.substringAfterLast("/")
                            _activeState.value = _localPuppetConfig.value?.states?.find { it.imageName == imageName }
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
                    // Keep the socket open, the state collector will send messages
                    incoming.receive() // This will suspend until the socket is closed
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