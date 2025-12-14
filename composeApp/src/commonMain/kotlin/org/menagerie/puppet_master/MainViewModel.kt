package org.menagerie.puppet_master

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class OperatingMode {
    ONLINE, OFFLINE
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
    val selectedThreshold: Float? = null,
    val preserveState: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UiState

        if (showStateAssignmentDialog != other.showStateAssignmentDialog) return false
        if (showOverwriteConfirmDialog != other.showOverwriteConfirmDialog) return false
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

    private val dataManager = PuppetDataManager(screenModelScope, context)
    val uploadsDir = dataManager.uploadsDir

    private val _thresholds = MutableStateFlow<Map<Float, PuppetStateInfo?>>(emptyMap())
    val thresholds: StateFlow<Map<Float, PuppetStateInfo?>> = _thresholds.asStateFlow()

    private val stateController = PuppetStateController(
        screenModelScope, dataManager, AudioProcessor(context),
        getUiState = { uiState.value },
        thresholds = thresholds
    )
    private val settingsRepository = SettingsRepository(context)

    val troupe: StateFlow<PuppetTroupe?> = dataManager.troupe
    val activePuppet: StateFlow<PuppetCharacter?> = dataManager.activePuppet
    val isListening: StateFlow<Boolean> = stateController.isListening
    val audioLevel: StateFlow<Float> = stateController.audioLevel
    val isBlinking: StateFlow<Boolean> = stateController.isBlinking

    private val _serverState = MutableStateFlow<ServerState?>(null)
    private val serverState: StateFlow<ServerState?> = _serverState.asStateFlow()

    private val _selectedState = MutableStateFlow<PuppetStateInfo?>(null)
    val selectedState: StateFlow<PuppetStateInfo?> = _selectedState.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _operatingMode = MutableStateFlow(OperatingMode.OFFLINE)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode.asStateFlow()

    private val _isAudienceCheckForced = MutableStateFlow(false)
    val isAudienceCheckForced: StateFlow<Boolean> = _isAudienceCheckForced.asStateFlow()

    private val client = HttpClient { install(WebSockets) }
    private var serverStateJob: Job? = null
    private var clientControlSocketJob: Job? = null

    private val specialEffectsController = SpecialEffectsController()
    val animationState: StateFlow<AnimationState>
        get() = _animationState
    private val _animationState = MutableStateFlow(AnimationState())
    private var animationJob: Job? = null
    private var effectStartTime = 0L

    private val _normalizedMousePosition = MutableStateFlow<SerializableOffset?>(null)
    val normalizedMousePosition: StateFlow<SerializableOffset?> = _normalizedMousePosition.asStateFlow()

    val activeState: StateFlow<PuppetStateInfo?>
    val activeSpecialEffect: StateFlow<ActiveSpecialEffect?>
    val displayedImageName: StateFlow<String?>

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; allowStructuredMapKeys = true }

    init {
        _settings.value = settingsRepository.loadSettings()
        _operatingMode.value = if (settings.value.startOffline) OperatingMode.OFFLINE else OperatingMode.ONLINE

        screenModelScope.launch {
            activePuppet.collect { puppet ->
                _thresholds.value = puppet?.thresholds ?: emptyMap()
                _selectedState.value?.let { selected ->
                    _selectedState.value = puppet?.states?.find { it.name == selected.name }
                }
            }
        }
        
        val localActiveState = stateController.activeState
        val localActiveSpecialEffect = stateController.activeSpecialEffect
        val localDisplayedImageName = stateController.displayedImageName

        val modeFlow = operatingMode.combine(isPublishing) { mode, isPublishing ->
            Pair(mode, isPublishing)
        }

        activeState = modeFlow.flatMapLatest { (mode, isPublishing) ->
            if (mode == OperatingMode.ONLINE && !isPublishing) {
                serverState.map { it?.puppetStateInfo }
            } else {
                localActiveState
            }
        }.stateIn(screenModelScope, SharingStarted.Lazily, localActiveState.value)

        activeSpecialEffect = modeFlow.flatMapLatest { (mode, isPublishing) ->
            if (mode == OperatingMode.ONLINE && !isPublishing) {
                serverState.map { it?.puppetStateInfo?.appliedEffect?.let { ActiveSpecialEffect(it) } }
            } else {
                localActiveSpecialEffect
            }
        }.stateIn(screenModelScope, SharingStarted.Lazily, localActiveSpecialEffect.value)

        displayedImageName = modeFlow.flatMapLatest { (mode, isPublishing) ->
            if (mode == OperatingMode.ONLINE && !isPublishing) {
                serverState.map { it?.puppetStateInfo?.imageName }
            } else {
                localDisplayedImageName
            }
        }.stateIn(screenModelScope, SharingStarted.Lazily, localDisplayedImageName.value)

        screenModelScope.launch {
            activeState.collect { state ->
                animationJob?.cancel()
                val effect = state?.appliedEffect
                val startTime = if (operatingMode.value == OperatingMode.ONLINE && !isPublishing.value) {
                    serverState.value?.effectStartTime ?: 0L
                } else {
                    System.currentTimeMillis()
                }

                if (effect != null) {
                    effectStartTime = startTime
                    animationJob = launch {
                        while (true) {
                            val elapsedTime = System.currentTimeMillis() - effectStartTime
                            _animationState.value = calculateAnimationState(effect, elapsedTime)
                            kotlinx.coroutines.delay(16) // roughly 60 fps
                        }
                    }
                } else {
                    _animationState.value = AnimationState()
                }
            }
        }
    }

    private fun calculateAnimationState(effect: SpecialEffect, elapsedTime: Long): AnimationState {
        val scaleX = specialEffectsController.getAnimatedScale(effect.scaleX, effect.scaleSpeed, elapsedTime)
        val scaleY = specialEffectsController.getAnimatedScale(effect.scaleY, effect.scaleSpeed, elapsedTime)
        val rotation = specialEffectsController.getRotation(effect.spinSpeed, effect.spinDirection, elapsedTime)
        val (translationX, translationY) = specialEffectsController.getVibration(
            effect.vibrationDistance,
            effect.vibrationSpeed,
            elapsedTime,
            1920f // assuming a default width, this might need to be configurable
        )
        return AnimationState(
            rotation = rotation,
            scaleX = scaleX,
            scaleY = scaleY,
            translationX = translationX,
            translationY = translationY,
            glowColor = effect.glowColor ?: 0xFFFFFFFF.toInt(),
            glowIntensity = effect.glowIntensity ?: 0f
        )
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        stateController.onKeyEvent(keyEvent)
    }

    fun updateSettings(newSettings: SettingsModel) {
        _settings.value = newSettings
        settingsRepository.saveSettings(newSettings)
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
                client.webSocket(method = HttpMethod.Get, host = settings.value.serverIpAddress, port = SERVER_PORT, path = "/obs") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            _serverState.value = json.decodeFromString<ServerState>(frame.readText())
                        }
                    }
                }
            } catch (e: Exception) { /* Handle error */
            }
        }
    }

    private fun startClientControl() {
        clientControlSocketJob = screenModelScope.launch {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = settings.value.serverIpAddress,
                    port = SERVER_PORT,
                    path = "/client-control"
                ) {
                    combine(
                        stateController.activeState,
                        stateController.displayedImageName,
                        normalizedMousePosition
                    ) { state, imageName, mousePosition ->
                        val currentState = state?.copy(
                            imageName = imageName ?: state.imageName,
                            blinkImageName = state.blinkImageName,
                            minBlinkRate = state.minBlinkRate,
                            maxBlinkRate = state.maxBlinkRate,
                            appliedEffect = state.appliedEffect
                        )
                        val mousePos = if (state?.eyeState?.eyes?.followCursor == true && mousePosition != null) {
                            MousePosition(x = mousePosition.x.toInt(), y = mousePosition.y.toInt())
                        } else {
                            null
                        }
                        ServerState(
                            puppetStateInfo = currentState,
                            effectStartTime = if (state?.appliedEffect != null) effectStartTime else null,
                            mousePosition = mousePos
                        )
                    }.collectLatest { serverState ->
                        send(json.encodeToString(serverState))
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun stopClientControl() {
        clientControlSocketJob?.cancel()
    }

    fun onNormalizedMousePositionChanged(position: SerializableOffset?) {
        if(activeState.value?.eyeState?.eyes?.followCursor?: false) {
            _normalizedMousePosition.value = position
        }
    }
}
