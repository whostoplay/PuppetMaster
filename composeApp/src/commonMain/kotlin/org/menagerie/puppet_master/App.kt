package org.menagerie.puppet_master

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.controls.ColorPicker
import org.menagerie.puppet_master.controls.ControlDrawer
import org.menagerie.puppet_master.controls.DraggableSplitter
import org.menagerie.puppet_master.controls.FilePicker
import org.menagerie.puppet_master.controls.FileSaver
import org.menagerie.puppet_master.controls.ModeControls
import org.menagerie.puppet_master.controls.PuppetControls
import org.menagerie.puppet_master.controls.ServerControls
import org.menagerie.puppet_master.controls.SpecialEffectsUI
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.navigation.AppNavigator
import org.menagerie.puppet_master.navigation.EyeContactScreen
import org.menagerie.puppet_master.navigation.SettingsScreen
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.states.StateCreation
import org.menagerie.puppet_master.states.StateEditor

@Composable
fun App() {
    AppNavigator()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppContent(viewModel: MainViewModel, window: Any?) {
    val navigator = LocalNavigator.currentOrThrow
    val context = getContext()
    val troupe by viewModel.troupe.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val displayedImageName by viewModel.displayedImageName.collectAsState()
    val activeState by viewModel.activeState.collectAsState()
    val isBlinking by viewModel.isBlinking.collectAsState()
    val activeSpecialEffect by viewModel.activeSpecialEffect.collectAsState()
    val focusRequester = remember { FocusRequester() }

    var showPermissionRequest by remember { mutableStateOf(false) }
    if (showPermissionRequest) {
        RequestAudioPermission { granted ->
            if (granted) {
                viewModel.toggleListening()
            }
            showPermissionRequest = false
        }
    }

    val isDesktop = isDesktop()
    var showControls by remember { mutableStateOf(isDesktop) }
    var controlsLocked by remember { mutableStateOf(!isDesktop) }
    val isHoveringOn = remember { mutableStateMapOf<String, Boolean>() }
    val isHoveringOnControls = isHoveringOn.values.any { it }
    val controlsAlpha by animateFloatAsState(if (showControls || controlsLocked) 1f else 0f)

    var idleImageData by remember { mutableStateOf<ByteArray?>(null) }

    var showPuppetImportPicker by remember { mutableStateOf(false) }
    var showTroupeLoadPicker by remember { mutableStateOf(false) }
    var showPuppetExportSaver by remember { mutableStateOf(false) }
    val textFieldFocusStates = remember { mutableStateMapOf<String, Boolean>() }
    val isTextFieldFocused = textFieldFocusStates.values.any { it }

    FilePicker(show = showPuppetImportPicker, fileExtensions = listOf("puppet")) { filePath ->
        if (filePath != null) {
            viewModel.importPuppet(filePath)
        }
        showPuppetImportPicker = false
    }

    FilePicker(show = showTroupeLoadPicker, fileExtensions = listOf("troupe")) { filePath ->
        if (filePath != null) {
            viewModel.loadTroupeFromFile(filePath)
        }
        showTroupeLoadPicker = false
    }

    FileSaver(show = showPuppetExportSaver, defaultFileName = "${activePuppet?.name}.puppet", fileExtensions = listOf("puppet")) { filePath ->
        if (filePath != null) {
            activePuppet?.let { viewModel.exportPuppet(it.name, filePath) }
        }
        showPuppetExportSaver = false
    }

    LaunchedEffect(activePuppet) {
        val idleState = activePuppet?.states?.find { it.name == "idle" } ?: activePuppet?.states?.firstOrNull()
        if (idleState != null) {
            idleImageData = viewModel.getImageData(idleState.imageName)
        } else {
            idleImageData = null
        }
    }
    val idleImage = idleImageData?.toImageBitmap()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isHoveringOnControls) {
        if (isHoveringOnControls) {
            showControls = true
        }
    }

    LaunchedEffect(showControls, isHoveringOnControls, controlsLocked) {
        if (showControls && !isHoveringOnControls && !controlsLocked) {
            delay(1500)
            if (!isHoveringOnControls && !controlsLocked) {
                showControls = false
            }
        }
    }

    if (uiState.showStateAssignmentDialog && uiState.selectedThreshold != null) {
        AlertDialog(
            onDismissRequest = { viewModel.hideStateAssignmentDialog() },
            title = { Text("Assign State to Threshold") },
            text = {
                LazyColumn {
                    items(activePuppet?.states.orEmpty()) { state ->
                        Text(
                            text = state.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.assignStateToThreshold(
                                        uiState.selectedThreshold!!,
                                        state
                                    )
                                }
                                .padding(8.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.hideStateAssignmentDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showOverwriteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideOverwriteConfirmDialog() },
            title = { Text("Overwrite State?") },
            text = { Text("A state with this name already exists. Do you want to overwrite it?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.forceCreateNewState()
                        viewModel.hideOverwriteConfirmDialog()
                    }
                ) {
                    Text("Overwrite")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideOverwriteConfirmDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showConnectionErrorDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissConnectionErrorDialog() },
            title = { Text("Connection Failed") },
            text = { Text("Could not connect to the server. Would you like to try again?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.connectAndSync()
                        viewModel.dismissConnectionErrorDialog()
                    }
                ) {
                    Text("Try Again")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.setOperatingMode(OperatingMode.OFFLINE)
                    viewModel.dismissConnectionErrorDialog()
                }) {
                    Text("Work Offline")
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { controlsLocked = !controlsLocked },
                    onTap = { showControls = true } // Always show on single tap
                )
            }
            .focusRequester(focusRequester)
            .onKeyEvent {
                if (!isTextFieldFocused) {
                    viewModel.onKeyEvent(it)
                    if (isDesktop) {
                        if (settings.toggleListenHotkey.isHotkey(it)) {
                            viewModel.toggleListening()
                            true
                        } else if (settings.togglePublishingHotkey.isHotkey(it)) {
                            viewModel.setPublishing(!isPublishing)
                            true
                        } else if (settings.toggleOnlineHotkey.isHotkey(it)) {
                            viewModel.toggleOperatingMode()
                            true
                        } else if (settings.toggleFocusHotkey.isHotkey(it)) {
                            viewModel.toggleFocus()
                            true
                        } else if (settings.checkAudienceHotkey.isHotkey(it)) {
                            viewModel.toggleForceAudienceCheck()
                            true
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        var leftPanelWidth by remember { mutableFloatStateOf(1 / 3f) }
        var rightPanelWidth by remember { mutableFloatStateOf(1 / 3f) }

        if (idleImage != null) {
            key(activePuppet) {
                LivePreview(
                    operatingMode = operatingMode,
                    puppetState = activeState,
                    isBlinking = isBlinking,
                    uploadsDir = viewModel.uploadsDir,
                    backgroundColor = uiState.backgroundColor,
                    serverIp = settings.serverIpAddress,
                    activeSpecialEffect = activeSpecialEffect,
                    window = window,
                    isAudienceCheckForced = viewModel.isAudienceCheckForced.collectAsState().value,
                    displayedImageName = displayedImageName,
                    idleImage = idleImage,
                    onFocusPointUpdate = { offset ->
                        viewModel.onNormalizedMousePositionChanged(offset)
                    }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (troupe == null) {
                    Text("Create a puppet or load a troupe to get started.")
                } else  {
                    Text("Load a State Image.")
                }
            }
        }

        Box(modifier = Modifier.graphicsLayer(alpha = controlsAlpha).fillMaxSize()) {
            if (isLandscape || isDesktop) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        modifier = Modifier.fillMaxHeight().weight(leftPanelWidth),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .onHover { isHoveringOn["leftPanel"] = it }
                        ) {
                            PuppetControls(
                                troupe = troupe,
                                activePuppet = activePuppet,
                                onPuppetSelected = { viewModel.setActivePuppet(it) },
                                onPuppetCreated = viewModel::createNewPuppet,
                                onImportPuppet = { showPuppetImportPicker = true },
                                onExportPuppet = { showPuppetExportSaver = true },
                                onRenameTroupe = viewModel::renameTroupe,
                                onLoadTroupe = { showTroupeLoadPicker = true },
                                onNewTroupeCreated = viewModel::createNewTroupe,
                                onActiveChange = { isHoveringOn["puppetControls"] = it },
                                onFocusChange = { textFieldFocusStates["puppetControls"] = it },
                                rootFocusRequester = focusRequester
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ModeControls(operatingMode, viewModel::setOperatingMode)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ServerControls(
                                operatingMode,
                                isPublishing,
                                isListening,
                                { viewModel.setPublishing(it) },
                                {
                                    if (hasAudioPermission(context)) {
                                        viewModel.toggleListening()
                                    } else {
                                        showPermissionRequest = true
                                    }
                                })
                            if (isListening) {
                                VolumeIndicator(
                                    level = audioLevel,
                                    modifier = Modifier.fillMaxWidth().padding(8.dp).size(20.dp),
                                    thresholds = thresholds,
                                    onAddThreshold = { newThreshold ->
                                        viewModel.addThreshold(newThreshold)
                                        viewModel.showStateAssignmentDialog(newThreshold)
                                    },
                                    onUpdateThreshold = viewModel::updateThreshold,
                                    onThresholdSelected = { threshold ->
                                        viewModel.showStateAssignmentDialog(
                                            threshold
                                        )
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Button(
                                onClick = {
                                    navigator.push(
                                        EyeContactScreen(
                                            activePuppet,
                                            viewModel
                                        )
                                    )
                                }, modifier = Modifier.align(
                                    Alignment.CenterHorizontally
                                )
                            ) {
                                Text("Eye Contact")
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Button(
                                onClick = { navigator.push(SettingsScreen(viewModel)) },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text("Settings")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    DraggableSplitter(
                        onDelta = { delta ->
                            val newWidth =
                                leftPanelWidth + (delta / this@BoxWithConstraints.maxWidth.value)
                            leftPanelWidth = newWidth.coerceIn(0.2f, 0.5f)
                        },
                        modifier = Modifier.onHover { isHoveringOn["leftDrag"] = it })

                    Spacer(
                        modifier = Modifier.fillMaxHeight()
                            .weight((1f - leftPanelWidth - rightPanelWidth).coerceAtLeast(0.05f))
                    )

                    DraggableSplitter(
                        onDelta = { delta ->
                            val newWidth =
                                rightPanelWidth - (delta / (this@BoxWithConstraints.maxWidth.value / 2))
                            rightPanelWidth = newWidth.coerceIn(0.2f, 0.5f)
                        },
                        modifier = Modifier.onHover { isHoveringOn["rightDrag"] = it })

                    Surface(
                        modifier = Modifier.fillMaxHeight().weight(rightPanelWidth),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        LazyColumn(
                            modifier = Modifier.onHover { isHoveringOn["rightPanel"] = it }
                        ) {
                            val currentPuppet = activePuppet
                            val currentTroupe = troupe
                            if (currentPuppet != null && currentTroupe != null) {
                                item {
                                    StateCreation(
                                        modifier = Modifier.fillMaxWidth(),
                                        viewModel = viewModel,
                                        onActiveChange = { isHoveringOn["stateCreation"] = it },
                                        onFocusChange = { textFieldFocusStates["stateCreation"] = it },
                                        rootFocusRequester = focusRequester
                                    )
                                }
                                item { HorizontalDivider() }
                                item { Text(text = "States") }
                                item { HorizontalDivider() }
                                items(currentPuppet.states) { state ->
                                    Text(
                                        text = state.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectState(state) }
                                            .background(if (state == selectedState) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                            .padding(8.dp)
                                    )
                                }
                                item {
                                    selectedState?.let { state ->
                                        StateEditor(
                                            modifier = Modifier.fillMaxWidth(),
                                            selectedState = state,
                                            specialEffectsManager = currentTroupe.specialEffectsManager,
                                            onStateUpdated = { viewModel.updatePuppetState(it) },
                                            onStateHotkeyChanged = viewModel::updateStateHotkey
                                        )
                                    }
                                }
                                item { HorizontalDivider() }
                                item {
                                    SpecialEffectsUI(
                                        specialEffectsManager = currentTroupe.specialEffectsManager,
                                        onSpecialEffectsManagerChanged = viewModel::onSpecialEffectsManagerChanged,
                                        activePuppet = currentPuppet,
                                        uploadsDir = viewModel.uploadsDir,
                                        preserveState = uiState.preserveState,
                                        onPreserveStateChanged = viewModel::onPreserveStateChanged,
                                        window = window,
                                        onFocusChange = { textFieldFocusStates["specialEffects"] = it },
                                        rootFocusRequester = focusRequester,
                                        backgroundColor = uiState.backgroundColor,
                                    )
                                 }
                            } else {
                                item {
                                    Box(
                                        modifier = Modifier.fillParentMaxSize().padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("Create or select a puppet to get started.")
                                    }
                                }
                            }
                        }
                    }
                }
            } else { // Portrait
                var showLeftDrawer by remember { mutableStateOf(false) }
                var showRightDrawer by remember { mutableStateOf(false) }

                ControlDrawer(
                    show = showLeftDrawer,
                    onDismissRequest = { showLeftDrawer = false }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        PuppetControls(
                            troupe = troupe,
                            activePuppet = activePuppet,
                            onPuppetSelected = { viewModel.setActivePuppet(it) },
                            onPuppetCreated = viewModel::createNewPuppet,
                            onImportPuppet = { showPuppetImportPicker = true },
                            onExportPuppet = { showPuppetExportSaver = true },
                            onRenameTroupe = viewModel::renameTroupe,
                            onLoadTroupe = { showTroupeLoadPicker = true },
                            onNewTroupeCreated = viewModel::createNewTroupe,
                            onActiveChange = { isHoveringOn["puppetControlsPortrait"] = it },
                            onFocusChange = { textFieldFocusStates["puppetControlsPortrait"] = it },
                            rootFocusRequester = focusRequester
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ModeControls(operatingMode, viewModel::setOperatingMode)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ServerControls(
                            operatingMode,
                            isPublishing,
                            isListening,
                            { viewModel.setPublishing(it) },
                            {
                                if (hasAudioPermission(context)) {
                                    viewModel.toggleListening()
                                } else {
                                    showPermissionRequest = true
                                }
                            })
                        if (isListening) {
                            VolumeIndicator(
                                level = audioLevel,
                                modifier = Modifier.fillMaxWidth().padding(8.dp).size(20.dp),
                                thresholds = thresholds,
                                onAddThreshold = { newThreshold ->
                                    viewModel.addThreshold(newThreshold)
                                    viewModel.showStateAssignmentDialog(newThreshold)
                                },
                                onUpdateThreshold = viewModel::updateThreshold,
                                onThresholdSelected = { threshold ->
                                    viewModel.showStateAssignmentDialog(
                                        threshold
                                    )
                                }
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) })
                    }
                }

                ControlDrawer(
                    show = showRightDrawer,
                    onDismissRequest = { showRightDrawer = false }
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().padding(16.dp)
                    ) {
                        val currentPuppet = activePuppet
                        val currentTroupe = troupe
                        if (currentPuppet != null && currentTroupe != null) {
                            item {
                                StateCreation(
                                    modifier = Modifier.fillMaxWidth(),
                                    viewModel = viewModel,
                                    onActiveChange = { isHoveringOn["stateCreationPortrait"] = it },
                                    onFocusChange = { textFieldFocusStates["stateCreationPortrait"] = it },
                                    rootFocusRequester = focusRequester
                                )
                            }
                            item { HorizontalDivider() }
                            item { Text(text = "States") }
                            item { HorizontalDivider() }
                            items(currentPuppet.states) { state ->
                                Text(
                                    text = state.name,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectState(state) }
                                        .background(if (state == selectedState) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                        .padding(8.dp)
                                )
                            }
                            item {
                                selectedState?.let { state ->
                                    StateEditor(
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedState = state,
                                        specialEffectsManager = currentTroupe.specialEffectsManager,
                                        onStateUpdated = { viewModel.updatePuppetState(it) },
                                        onStateHotkeyChanged = viewModel::updateStateHotkey
                                    )
                                }
                            }
                            item {
                                SpecialEffectsUI(
                                    specialEffectsManager = currentTroupe.specialEffectsManager,
                                    onSpecialEffectsManagerChanged = viewModel::onSpecialEffectsManagerChanged,
                                    activePuppet = currentPuppet,
                                    uploadsDir = viewModel.uploadsDir,
                                    preserveState = uiState.preserveState,
                                    onPreserveStateChanged = viewModel::onPreserveStateChanged,
                                    window = window,
                                    onFocusChange = { textFieldFocusStates["specialEffectsPortrait"] = it },
                                    rootFocusRequester = focusRequester,
                                    backgroundColor = uiState.backgroundColor
                                )
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier.fillParentMaxSize().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("Create or select a puppet to get started.")
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = { showLeftDrawer = true }) { Text("Puppet Controls") }
                    Button(onClick = { showRightDrawer = true }) { Text("State Controls") }
                    Button(onClick = {
                        navigator.push(
                            EyeContactScreen(
                                activePuppet,
                                viewModel
                            )
                        )
                    }) { Text("Eye Contact") }
                    Button(onClick = { navigator.push(SettingsScreen(viewModel)) }) { Text("Settings") }
                }
            }
        }
    }
}

@Composable
expect fun ByteArray.toImageBitmap(): ImageBitmap
