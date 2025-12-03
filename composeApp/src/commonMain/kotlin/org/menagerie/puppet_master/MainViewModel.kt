package org.menagerie.puppet_master

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
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

class MainViewModel(context: Any) : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val dataManager = PuppetDataManager(viewModelScope, context)
    val uploadsDir = dataManager.uploadsDir

    private val _thresholds = MutableStateFlow<Map<Float, PuppetStateInfo?>>(emptyMap())
    val thresholds: StateFlow<Map<Float, PuppetStateInfo?>> = _thresholds.asStateFlow()

    private val stateController = PuppetStateController(
        viewModelScope, dataManager, AudioProcessor(context),
        getUiState = { uiState.value },
        thresholds = thresholds
    )
    private val settingsRepository = SettingsRepository(context)

    val troupe: StateFlow<PuppetTroupe?> = dataManager.troupe
    val activePuppet: StateFlow<PuppetCharacter?> = dataManager.activePuppet
    val activeState: StateFlow<PuppetStateInfo?> = stateController.activeState
    val activeSpecialEffect: StateFlow<ActiveSpecialEffect?> = stateController.activeSpecialEffect
    val displayedImageName: StateFlow<String?> = stateController.displayedImageName
    val isListening: StateFlow<Boolean> = stateController.isListening
    val audioLevel: StateFlow<Float> = stateController.audioLevel

    private val _serverImageName = MutableStateFlow<String?>(null)
    val serverImageName: StateFlow<String?> = _serverImageName.asStateFlow()

    private val _serverSpecialEffect = MutableStateFlow<ActiveSpecialEffect?>(null)
    val serverSpecialEffect: StateFlow<ActiveSpecialEffect?> = _serverSpecialEffect.asStateFlow()

    private val _selectedState = MutableStateFlow<PuppetStateInfo?>(null)
    val selectedState: StateFlow<PuppetStateInfo?> = _selectedState.asStateFlow()

    private val _serverIpAddress = MutableStateFlow(DEFAULT_SERVER_HOST)
    val serverIpAddress: StateFlow<String> = _serverIpAddress.asStateFlow()

    private val _isPublishing = MutableStateFlow(false)
    val isPublishing: StateFlow<Boolean> = _isPublishing.asStateFlow()

    private val _operatingMode = MutableStateFlow(OperatingMode.OFFLINE)
    val operatingMode: StateFlow<OperatingMode> = _operatingMode.asStateFlow()

    private val client = HttpClient { install(WebSockets) }
    private var serverStateJob: Job? = null
    private var clientControlSocketJob: Job? = null

    init {
        _serverIpAddress.value = settingsRepository.loadIp()
        viewModelScope.launch {
            activePuppet.collect { puppet ->
                _thresholds.value = puppet?.thresholds ?: emptyMap()
                _selectedState.value?.let { selected ->
                    _selectedState.value = puppet?.states?.find { it.name == selected.name }
                }
            }
        }
    }

    suspend fun getImageData(imageName: String): ByteArray? {
        return dataManager.getImageData(imageName)
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
        _operatingMode.value = mode
        dataManager.setOperatingMode(mode)
        stateController.operatingMode = mode

        if (mode == OperatingMode.ONLINE) {
            if (_isPublishing.value) {
                setPublishing(false)
            }
            connectAndSync()
        } else { // OFFLINE
            serverStateJob?.cancel()
            stateController.onOffline()
        }
    }

    fun onServerIpAddressChanged(ipAddress: String) {
        _serverIpAddress.value = ipAddress
        settingsRepository.saveIp(ipAddress)
    }

    fun connectAndSync() {
        dataManager.connectAndSync(serverIpAddress.value)
        observeServerState()
    }

    fun setActivePuppet(name: String) {
        dataManager.setActivePuppet(name)
    }

    fun createNewPuppet(name: String) {
        dataManager.createNewPuppet(name)
    }

    fun setPublishing(isPublishing: Boolean) {
        if (_operatingMode.value == OperatingMode.OFFLINE) return

        _isPublishing.value = isPublishing
        stateController.isPublishing = isPublishing

        if (isPublishing) {
            dataManager.publishTroupe(serverIpAddress.value)
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
            serverIpAddress.value
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

    fun updateBlinkRate(state: PuppetStateInfo, blinkRate: LongRange) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == state.name) it.copy(minBlinkRate = blinkRate.first, maxBlinkRate = blinkRate.last) else it
            }
            val newThresholds = character.thresholds.mapValues { (_, value) ->
                if (value?.name == state.name) value.copy(minBlinkRate = blinkRate.first, maxBlinkRate = blinkRate.last) else value
            }
            character.copy(states = newStates, thresholds = newThresholds)
        }
    }

    fun updateAppliedEffect(state: PuppetStateInfo, effect: SpecialEffect?) {
        dataManager.updatePuppet(dataManager.activePuppet.value!!.name) { character ->
            val newStates = character.states.map {
                if (it.name == state.name) it.copy(appliedEffect = effect) else it
            }
            val newThresholds = character.thresholds.mapValues { (_, value) ->
                if (value?.name == state.name) value.copy(appliedEffect = effect) else value
            }
            character.copy(states = newStates, thresholds = newThresholds)
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

    fun onSpecialEffectUpdated() {
        viewModelScope.launch {
            troupe.value?.let { dataManager.saveTroupe(it) }
        }
    }

    fun onPreserveStateChanged(preserveState: Boolean) {
        _uiState.value = _uiState.value.copy(preserveState = preserveState)
    }

    private fun persistThresholds() {
        dataManager.activePuppet.value?.let { puppet ->
            val thresholdsToSave = _thresholds.value.filterValues { it != null }.mapValues { it.value!! }
            dataManager.updatePuppet(puppet.name) { it.copy(thresholds = thresholdsToSave, lastUpdated = System.currentTimeMillis()) }
        }
    }

    private fun observeServerState() {
        stateController.stopBlinking()
        serverStateJob = viewModelScope.launch {
            try {
                client.webSocket(method = HttpMethod.Get, host = serverIpAddress.value, port = SERVER_PORT, path = "/obs") {
                    for (frame in incoming) {
                        if (frame is Frame.Text) {
                            val serverState = Json.decodeFromString<ServerState>(frame.readText())
                            val stateInfo = serverState.puppetStateInfo
                            _serverImageName.value = stateInfo?.imageName
                            _serverSpecialEffect.value = stateInfo?.appliedEffect?.let { ActiveSpecialEffect(it) }
                            println(stateInfo)
                        }
                    }
                }
            } catch (e: Exception) { /* Handle error */
            }
        }
    }

    private fun startClientControl() {
        clientControlSocketJob = viewModelScope.launch {
            try {
                client.webSocket(
                    method = HttpMethod.Get,
                    host = serverIpAddress.value,
                    port = SERVER_PORT,
                    path = "/client-control"
                ) {
                    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
                    activeState.combine(displayedImageName) { state, imageName ->
                        val currentState = state?.copy(
                            imageName = imageName ?: state.imageName,
                            blinkImageName = state.blinkImageName,
                            minBlinkRate = state.minBlinkRate,
                            maxBlinkRate = state.maxBlinkRate,
                            appliedEffect = state.appliedEffect
                        )
                        ServerState(currentState)
                    }.collectLatest { state ->
                        send(json.encodeToString(state))
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
}
