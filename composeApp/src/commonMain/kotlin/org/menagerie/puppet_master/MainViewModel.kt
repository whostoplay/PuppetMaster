package org.menagerie.puppet_master

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.sendSerialized
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.state_machine.GraphAction
import org.menagerie.puppet_master.state_machine.GraphExecutionContext
import org.menagerie.puppet_master.state_machine.GraphExecutor
import org.menagerie.puppet_master.state_machine.NodeGraph

enum class OperatingMode {
    ONLINE, OFFLINE
}

enum class ControlMode {
    DIRECT, STATE_MACHINE
}

data class UiState(
    val selectedImage: ByteArray? = null,
    val selectedImageName: String = "",
    val selectedBlinkImage: ByteArray? = null,
    val selectedBlinkImageName: String = "",
    val newStateName: String = "",
    val backgroundColor: Color = Color.Green,
    val showStateAssignmentDialog: Boolean = false,
    val showOverwriteConfirmDialog: Boolean = false,
    val showConnectionErrorDialog: Boolean = false,
    val selectedThreshold: Float? = null,
    val preserveState: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UiState

        if (showStateAssignmentDialog != other.showStateAssignmentDialog) return false
        if (showOverwriteConfirmDialog != other.showOverwriteConfirmDialog) return false
        if (showConnectionErrorDialog != other.showConnectionErrorDialog) return false
        if (selectedThreshold != other.selectedThreshold) return false
        if (!selectedImage.contentEquals(other.selectedImage)) return false
        if (selectedImageName != other.selectedImageName) return false
        if (!selectedBlinkImage.contentEquals(other.selectedBlinkImage)) return false
        if (selectedBlinkImageName != other.selectedBlinkImageName) return false
        if (newStateName != other.newStateName) return false
        if (backgroundColor != other.backgroundColor) return false
        if (preserveState != other.preserveState) return false

        return true
    }

    override fun hashCode(): Int {
        var result = showStateAssignmentDialog.hashCode()
        result = 31 * result + showOverwriteConfirmDialog.hashCode()
        result = 31 * result + showConnectionErrorDialog.hashCode()
        result = 31 * result + (selectedThreshold?.hashCode() ?: 0)
        result = 31 * result + (selectedImage?.contentHashCode() ?: 0)
        result = 31 * result + selectedImageName.hashCode()
        result = 31 * result + (selectedBlinkImage?.contentHashCode() ?: 0)
        result = 31 * result + selectedBlinkImageName.hashCode()
        result = 31 * result + newStateName.hashCode()
        result = 31 * result + backgroundColor.hashCode()
        result = 31 * result + preserveState.hashCode()
        return result
    }
}

class MainViewModel(context: Any) : ScreenModel {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _settings = MutableStateFlow(SettingsModel())
    val settings: StateFlow<SettingsModel> = _settings.asStateFlow()

    val language: StateFlow<Strings.Language> = settings.map { it.language }
        .stateIn(screenModelScope, SharingStarted.Eagerly, settings.value.language)

    private val dataManager = PuppetDataManager(screenModelScope, context)
    val uploadsDir = dataManager.uploadsDir

    private val _thresholds = MutableStateFlow<Map<Float, PuppetStateInfo?>>(emptyMap())
    val thresholds: StateFlow<Map<Float, PuppetStateInfo?>> = _thresholds.asStateFlow()

    private val audioProcessor = AudioProcessor(context)
    private val stateController = PuppetStateController(
        screenModelScope, dataManager, audioProcessor,
        getUiState = { uiState.value },
        thresholds = thresholds
    )
    private val settingsRepository = SettingsRepository(context)

    val troupe: StateFlow<PuppetTroupe?> = dataManager.troupe
    val activePuppet: StateFlow<PuppetCharacter?> = dataManager.activePuppet

    private val _idleImage = MutableStateFlow<ImageBitmap?>(null)
    val idleImage: StateFlow<ImageBitmap?> = _idleImage.asStateFlow()

    val isListening: StateFlow<Boolean> = stateController.isListening
    val rawAudioLevel: StateFlow<Float> = stateController.rawAudioLevel
    val audioLevel: StateFlow<Float> = stateController.audioLevel
    val isBlinking: StateFlow<Boolean> = stateController.isBlinking
    val connectionState: StateFlow<ConnectionState> = dataManager.connectionState

    private val _serverState = MutableStateFlow<ServerState?>(null)
    private val serverState: StateFlow<ServerState?> = _serverState.asStateFlow()

    private val _selectedState = MutableStateFlow<PuppetStateInfo?>(null)
    val selectedState: StateFlow<PuppetStateInfo?> = _selectedState.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _operatingMode = MutableStateFlow(OperatingMode.OFFLINE)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode.asStateFlow()

    private val _controlMode = MutableStateFlow(ControlMode.DIRECT)
    val controlMode: StateFlow<ControlMode> = _controlMode.asStateFlow()

    private val _isAudienceCheckForced = MutableStateFlow(false)
    val isAudienceCheckForced: StateFlow<Boolean> = _isAudienceCheckForced.asStateFlow()

    private val _sensitivity = MutableStateFlow(1.0f)
    val sensitivity: StateFlow<Float> = _sensitivity.asStateFlow()

    val frequencyData: StateFlow<FloatArray> = stateController.frequencyData
    private val _frequencyPeaks = MutableStateFlow<List<Pair<Float, Float>>>(emptyList())
    val frequencyPeaks: StateFlow<List<Pair<Float, Float>>> = _frequencyPeaks.asStateFlow()

    private val client = HttpClient {
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true })
        }
        install(ContentNegotiation) {
            json(Json { isLenient = true; ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true })
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 15000
            socketTimeoutMillis = 15000
        }
    }
    private var serverStateJob: Job? = null
    private var clientControlSocketJob: Job? = null

    private var effectStartTime = 0L

    private val _normalizedMousePosition = MutableStateFlow<SerializableOffset?>(null)
    val normalizedMousePosition: StateFlow<SerializableOffset?> = _normalizedMousePosition.asStateFlow()

    val activeState: StateFlow<PuppetStateInfo?>
    val activeSpecialEffect: StateFlow<ActiveSpecialEffect?>
    val displayedImageName: StateFlow<String?>
    private var graphExecutor: GraphExecutor? = null
    private val _lastPressedKey = MutableStateFlow<Hotkey?>(null)

    init {
        _settings.value = settingsRepository.loadSettings()
        _operatingMode.value = if (settings.value.startOffline) OperatingMode.OFFLINE else OperatingMode.ONLINE

        screenModelScope.launch {
            troupe.collect { troupe ->
                graphExecutor = troupe?.nodeGraph?.let { GraphExecutor(it) }
            }
        }

        screenModelScope.launch {
            activePuppet.collect { puppet ->
                _thresholds.value = puppet?.thresholds ?: emptyMap()
                _selectedState.value?.let { selected ->
                    _selectedState.value = puppet?.states?.find { it.name == selected.name }
                }
                val idleState =
                    puppet?.states?.find { it.name == Constants.Puppet.IDLE_STATE_NAME } ?: puppet?.states?.firstOrNull()
                if (idleState != null) {
                    val imageData = getImageData(idleState.imageName)
                    if (imageData != null) {
                        _idleImage.value = decodeToImageBitmap(imageData)
                    } else {
                        _idleImage.value = null
                    }
                } else {
                    _idleImage.value = null
                }
            }
        }

        screenModelScope.launch {
            connectionState.collect { state ->
                if (state == ConnectionState.FAILED) {
                    _uiState.value = _uiState.value.copy(showConnectionErrorDialog = true)
                }
            }
        }

        screenModelScope.launch {
            controlMode.collectLatest { mode ->
                if (mode == ControlMode.STATE_MACHINE) {
                    while (true) {
                        val level = audioLevel.value
                        val hotkey = _lastPressedKey.value
                        val context = GraphExecutionContext(
                            microphoneVolume = level,
                            hotKeyPressed = hotkey,
                            frequencyPeaks = _frequencyPeaks.value
                        )
                        val action = graphExecutor?.tick(context)
                        if (action is GraphAction.SetState) {
                            action.puppetId?.let { puppetId ->
                                if (puppetId != activePuppet.value?.name) {
                                    setActivePuppet(puppetId)
                                }
                            }
                            if(action.effect != null) {
                                stateController.setStateByNameWithEffect(action.stateName, action.effect)
                            } else {
                                stateController.setStateByName(action.stateName)
                            }
                        }
                        kotlinx.coroutines.delay(16) // roughly 60 fps
                    }
                }
            }
        }

        val localActiveState = stateController.activeState
        val localDisplayedImageName = stateController.displayedImageName

        val modeFlow = operatingMode.combine(isPublishing) { mode, isPublishing ->
            Pair(mode, isPublishing)
        }

        val stateWithCursorUpdates = localActiveState.combine(_normalizedMousePosition) { state, position ->
            if (state?.eyeState?.eyes?.followCursor == true) {
                state.copy(eyeState = state.eyeState!!.copy(cursorPosition = position))
            } else {
                state
            }
        }

        activeState = modeFlow.flatMapLatest { (mode, isPublishing) ->
            if (mode == OperatingMode.ONLINE && !isPublishing) {
                serverState.map { it?.puppetStateInfo }
            } else {
                stateWithCursorUpdates
            }
        }.stateIn(screenModelScope, SharingStarted.Lazily, localActiveState.value)

        displayedImageName = modeFlow.flatMapLatest { (mode, isPublishing) ->
            if (mode == OperatingMode.ONLINE && !isPublishing) {
                serverState.map { it?.puppetStateInfo?.imageName }
            } else {
                localDisplayedImageName
            }
        }.stateIn(screenModelScope, SharingStarted.Lazily, localDisplayedImageName.value)

        activeSpecialEffect = activeState.map { state ->
            val newEffect = state?.appliedEffect
            val startTime = if (operatingMode.value == OperatingMode.ONLINE && !isPublishing.value) {
                serverState.value?.effectStartTime
            } else {
                System.currentTimeMillis()
            }
            newEffect?.let { ActiveSpecialEffect(it, startTime ?: System.currentTimeMillis()) }
        }.stateIn(screenModelScope, SharingStarted.Lazily, null)

    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        if (controlMode.value == ControlMode.DIRECT) {
            stateController.onKeyEvent(keyEvent)
        } else if (controlMode.value == ControlMode.STATE_MACHINE) {
            val hotkey = Hotkey(
                keyEvent.key.keyCode,
                keyEvent.isShiftPressed,
                keyEvent.isCtrlPressed,
                keyEvent.isAltPressed,
            )
            if (keyEvent.type == KeyEventType.KeyDown) {
                _lastPressedKey.value = hotkey
            } else if (keyEvent.type == KeyEventType.KeyUp) {
                if (_lastPressedKey.value?.shallowEquals(hotkey) == true) {
                    _lastPressedKey.value = null
                }
            }
        }
    }

    fun onPeaksDetected(peaks: List<Pair<Float, Float>>) {
        _frequencyPeaks.value = peaks
        println(peaks.count())
    }

    fun updateSettings(newSettings: SettingsModel) {
        _settings.value = newSettings
        settingsRepository.saveSettings(newSettings)
    }

    fun setLastImageFolder(folder: String) {
        val newSettings = _settings.value.copy(lastImageFolder = folder)
        updateSettings(newSettings)
    }

    suspend fun getImageData(imageName: String): ByteArray? {
        return dataManager.getImageData(imageName)
    }

    fun uploadImageData(name: String, data: ByteArray) {
        screenModelScope.launch {
            dataManager.saveImage(name, data)
        }
    }

    fun onStateCreationChange(
        image: ByteArray?,
        imageName: String,
        blinkImage: ByteArray?,
        blinkImageName: String,
        stateName: String
    ) {
        _uiState.value = _uiState.value.copy(
            selectedImage = image,
            selectedImageName = imageName,
            selectedBlinkImage = blinkImage,
            selectedBlinkImageName = blinkImageName,
            newStateName = stateName
        )
    }

    fun setBackgroundColor(color: Color) {
        _uiState.value = _uiState.value.copy(backgroundColor = color)
    }

    fun showStateAssignmentDialog(threshold: Float) {
        _uiState.value = _uiState.value.copy(showStateAssignmentDialog = true, selectedThreshold = threshold)
    }

    fun hideStateAssignmentDialog() {
        _uiState.value = _uiState.value.copy(showStateAssignmentDialog = false, selectedThreshold = null)
    }

    fun showOverwriteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showOverwriteConfirmDialog = true)
    }

    fun hideOverwriteConfirmDialog() {
        _uiState.value = _uiState.value.copy(showOverwriteConfirmDialog = false)
    }

    fun dismissConnectionErrorDialog() {
        _uiState.value = _uiState.value.copy(showConnectionErrorDialog = false)
    }

    fun setOperatingMode(mode: OperatingMode) {
        if (mode == _operatingMode.value) return

        if (mode == OperatingMode.OFFLINE) {
            if (_isPublishing.value) {
                setPublishing(false)
            }
            serverStateJob?.cancel()
            clientControlSocketJob?.cancel()
            stateController.onOffline()
        } else { // ONLINE
            if (_isPublishing.value) {
                setPublishing(false)
            }
            connectAndSync()
        }

        _operatingMode.value = mode
        dataManager.setOperatingMode(mode)
        stateController.operatingMode = mode
    }

    fun setControlMode(mode: ControlMode) {
        if (mode == _controlMode.value) return

        if (mode == ControlMode.DIRECT) {
            graphExecutor?.reset()
        }

        _controlMode.value = mode
        stateController.controlMode = mode
    }

    fun toggleOperatingMode() {
        setOperatingMode(if (operatingMode.value == OperatingMode.OFFLINE) OperatingMode.ONLINE else OperatingMode.OFFLINE)
    }

    fun connectAndSync() {
        dataManager.connectAndSync(settings.value.serverIpAddress)
        observeServerState()
    }

    fun setActivePuppet(name: String) {
        dataManager.setActivePuppet(name)
    }

    fun createNewPuppet(name: String, troupeName: String? = null) {
        dataManager.createNewPuppet(name, troupeName)
    }

    fun createNewTroupe() {
        dataManager.createNewTroupe()
    }

    fun setPublishing(isPublishing: Boolean) {
        if (_operatingMode.value == OperatingMode.OFFLINE && isPublishing) return

        _isPublishing.value = isPublishing
        stateController.isPublishing = isPublishing

        if (isPublishing) {
            dataManager.publishTroupe(settings.value.serverIpAddress)
            serverStateJob?.cancel()
            startClientControl()
        } else {
            stopClientControl()
            if (operatingMode.value == OperatingMode.ONLINE) {
                observeServerState()
            }
        }
    }

    fun selectState(state: PuppetStateInfo) {
        _selectedState.value = state
    }

    fun selectStateByName(stateName: String) {
        val state = activePuppet.value?.states?.find { it.name == stateName } ?: return
        selectState(state)
    }

    fun onSaveOrUpdateStateClicked() {
        val stateName = _uiState.value.newStateName
        if (activePuppet.value?.states?.any { it.name == stateName } == true) {
            showOverwriteConfirmDialog()
        } else {
            forceCreateNewState()
        }
    }

    fun forceCreateNewState() {
        val uiState = _uiState.value
        dataManager.createNewState(
            uiState.newStateName,
            uiState.selectedImage!!,
            uiState.selectedImageName,
            uiState.selectedBlinkImage,
            uiState.selectedBlinkImageName,
            settings.value.serverIpAddress
        )
        _uiState.value = uiState.copy(
            selectedImage = null,
            selectedImageName = "",
            selectedBlinkImage = null,
            selectedBlinkImageName = "",
            newStateName = ""
        )
    }

    fun toggleListening() {
        stateController.toggleListening()
    }

    fun toggleForceAudienceCheck() {
        _isAudienceCheckForced.value = !_isAudienceCheckForced.value
    }

    fun toggleFocus() {
        val currentState = activeState.value ?: return
        val currentEyeState = currentState.eyeState ?: return
        val currentEyePair = currentEyeState.eyes

        val nextEyePair = when {
            // From Both False -> Focus on Game True, Follow Cursor False
            !currentEyePair.focusOnGame && !currentEyePair.followCursor -> {
                currentEyePair.copy(focusOnGame = true, followCursor = false)
            }
            // From Focus on Game True, Follow Cursor False -> Focus on Game False, Follow Cursor True
            currentEyePair.focusOnGame && !currentEyePair.followCursor -> {
                currentEyePair.copy(focusOnGame = false, followCursor = true)
            }
            // From Focus on Game False, Follow Cursor True -> Both False
            else -> {
                currentEyePair.copy(focusOnGame = false, followCursor = false)
            }
        }
        updateEyeState(currentState.name, currentEyeState.copy(eyes = nextEyePair))
    }

    fun updateBlinkRate(state: PuppetStateInfo, blinkRate: LongRange) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == state.name) it.copy(minBlinkRate = blinkRate.first, maxBlinkRate = blinkRate.last) else it
            }
            val newThresholds = character.thresholds.mapValues { (_, value) ->
                if (value.name == state.name) value.copy(minBlinkRate = blinkRate.first, maxBlinkRate = blinkRate.last) else value
            }
            character.copy(states = newStates, thresholds = newThresholds)
        }
    }

    fun updateStateHotkey(stateName: String, newHotkey: Hotkey) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == stateName) {
                    it.copy(hotkey = newHotkey)
                } else if (it.hotkey == newHotkey) {
                    it.copy(hotkey = null)
                } else {
                    it
                }
            }
            character.copy(states = newStates)
        }
    }

    fun updateAppliedEffect(state: PuppetStateInfo, effect: SpecialEffect?) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == state.name) it.copy(appliedEffect = effect) else it
            }
            val newThresholds = character.thresholds.mapValues { (_, value) ->
                if (value.name == state.name) value.copy(appliedEffect = effect) else value
            }
            character.copy(states = newStates, thresholds = newThresholds)
        }
    }

    fun updateEyeState(stateName: String, eyeState: EyeState) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == stateName) it.copy(eyeState = eyeState) else it
            }
            val newThresholds = character.thresholds.mapValues { (_, value) ->
                if (value.name == stateName) value.copy(eyeState = eyeState) else value
            }
            character.copy(states = newStates, thresholds = newThresholds)
        }
        if (isPublishing.value) {
            dataManager.publishTroupe(settings.value.serverIpAddress)
        }
    }

    fun updatePuppetState(newState: PuppetStateInfo) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == newState.name) newState else it
            }
            val newThresholds = character.thresholds.mapValues { (_, value) ->
                if (value.name == newState.name) newState else value
            }
            character.copy(states = newStates, thresholds = newThresholds)
        }
        if (isPublishing.value) {
            dataManager.publishTroupe(settings.value.serverIpAddress)
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
        hideStateAssignmentDialog()
    }

    fun onSpecialEffectsManagerChanged(manager: SpecialEffectsManager) {
        troupe.value?.let { currentTroupe ->
            val newTroupe = currentTroupe.copy(specialEffectsManager = manager)
            dataManager.saveTroupe(newTroupe)
        }
    }

    fun onSpecialEffectUpdated(effect: SpecialEffect) {
        screenModelScope.launch {
            troupe.value?.let {
                val updatedManager = it.specialEffectsManager.updateEffect(it.specialEffectsManager.activeEffectIndex, effect)
                dataManager.saveTroupe(it.copy(specialEffectsManager = updatedManager))
             }
        }
    }

    fun onPreserveStateChanged(preserveState: Boolean) {
        _uiState.value = _uiState.value.copy(preserveState = preserveState)
    }

    fun renameTroupe(newName: String) {
        dataManager.renameTroupe(newName)
    }

    fun renamePuppet(newName: String) {
        dataManager.renamePuppet(newName)
    }

    fun loadTroupeFromFile(filePath: String) {
        dataManager.loadTroupeFromFile(filePath)
    }

    fun exportPuppet(puppetName: String, exportPath: String) {
        dataManager.exportPuppet(puppetName, exportPath)
    }

    fun importPuppet(filePath: String, newTroupeName: String? = null) {
        dataManager.importPuppet(filePath, newTroupeName)
    }

    fun saveTroupeAs(filePath: String) {
        dataManager.saveTroupeAs(filePath)
    }

    private fun persistThresholds() {
        dataManager.activePuppet.value?.let { puppet ->
            val thresholdsToSave = _thresholds.value.filterValues { it != null }.mapValues { it.value!! }
            dataManager.updatePuppet(puppet.name) { it.copy(thresholds = thresholdsToSave, lastUpdated = System.currentTimeMillis()) }
        }
    }

    private fun observeServerState() {
        stateController.stopBlinking()
        serverStateJob = screenModelScope.launch {
            try {
                client.webSocketSession(method = HttpMethod.Get, host = settings.value.serverIpAddress, port = Constants.Server.PORT, path = "/obs").let { session ->
                    while (true) {
                        val serverState = session.incoming.receive() as? ServerState
                        _serverState.value = serverState
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun startClientControl() {
        clientControlSocketJob = screenModelScope.launch {
            try {
                client.webSocketSession(
                    method = HttpMethod.Get,
                    host = settings.value.serverIpAddress,
                    port = Constants.Server.PORT,
                    path = "/client-control"
                ).let { session ->
                    combine(
                        stateController.activeState,
                        stateController.displayedImageName,
                        _normalizedMousePosition
                    ) { state, imageName, position ->
                        val stateWithCursor = if (state?.eyeState?.eyes?.followCursor == true) {
                            state.copy(eyeState = state.eyeState!!.copy(cursorPosition = position))
                        } else {
                            state
                        }

                        val currentState = stateWithCursor?.copy(
                            imageName = imageName ?: stateWithCursor.imageName,
                            blinkImageName = stateWithCursor.blinkImageName,
                            minBlinkRate = stateWithCursor.minBlinkRate,
                            maxBlinkRate = stateWithCursor.maxBlinkRate,
                            appliedEffect = stateWithCursor.appliedEffect
                        )
                        ServerState(
                            puppetStateInfo = currentState,
                            effectStartTime = if (stateWithCursor?.appliedEffect != null) effectStartTime else null,
                        )
                    }.collectLatest { serverState ->
                        session.sendSerialized(serverState)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun stopClientControl() {
        clientControlSocketJob?.cancel()
    }

    fun onNormalizedMousePositionChanged(position: SerializableOffset?) {
        _normalizedMousePosition.value = position
    }

    fun updateNodeGraph(nodeGraph: NodeGraph) {
        dataManager.updateNodeGraph(nodeGraph)
        graphExecutor = GraphExecutor(nodeGraph)
    }

    fun onSensitivityChange(newSensitivity: Float) {
        _sensitivity.value = newSensitivity
    }
}
