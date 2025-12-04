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
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.controls.ColorPicker
import org.menagerie.puppet_master.controls.ControlDrawer
import org.menagerie.puppet_master.controls.ModeControls
import org.menagerie.puppet_master.controls.PuppetControls
import org.menagerie.puppet_master.controls.ServerControls
import org.menagerie.puppet_master.controls.SpecialEffectsUI
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.navigation.AppNavigator
import org.menagerie.puppet_master.navigation.EyeContactScreen
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.states.StateCreation
import org.menagerie.puppet_master.states.StateEditor

@Composable
fun App() {
    AppNavigator()
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AppContent() {
    val navigator = LocalNavigator.currentOrThrow
    val context = getContext()
    val viewModel = remember { MainViewModel(context) }
    val troupe by viewModel.troupe.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val displayedImageName by viewModel.displayedImageName.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val audioLevel by viewModel.audioLevel.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val serverIpAddress by viewModel.serverIpAddress.collectAsState()
    val activeSpecialEffect by viewModel.activeSpecialEffect.collectAsState()
    val serverImageName by viewModel.serverImageName.collectAsState()
    val serverSpecialEffect by viewModel.serverSpecialEffect.collectAsState()
    val focusManager = LocalFocusManager.current

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
                                .clickable { viewModel.assignStateToThreshold(uiState.selectedThreshold!!, state) }
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

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { controlsLocked = !controlsLocked },
                    onTap = { showControls = true } // Always show on single tap
                )
            }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move) {
                            showControls = true
                        }
                    }
                }
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        val panelWeight = 1 / 3f

        val imageName = if (operatingMode == OperatingMode.ONLINE && !isPublishing) serverImageName else displayedImageName
        val specialEffect = if (operatingMode == OperatingMode.ONLINE && !isPublishing) serverSpecialEffect else activeSpecialEffect

        LivePreview(
            operatingMode = operatingMode,
            displayedImageName = imageName,
            uploadsDir = viewModel.uploadsDir,
            backgroundColor = uiState.backgroundColor,
            serverIp = serverIpAddress,
            activeSpecialEffect = specialEffect
        )

        Box(modifier = Modifier.graphicsLayer(alpha = controlsAlpha).fillMaxSize()) {
            if (isLandscape || isDesktop) {
                Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Surface(
                        modifier = Modifier.fillMaxHeight().weight(panelWeight),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                                .onHover { isHoveringOn["leftPanel"] = it }
                        ) {
                            PuppetControls(troupe, activePuppet, viewModel::setActivePuppet, viewModel::createNewPuppet)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ModeControls(operatingMode, viewModel::setOperatingMode)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            TextField(
                                value = serverIpAddress,
                                onValueChange = viewModel::onServerIpAddressChanged,
                                label = { Text("Server IP Address") },
                                singleLine = true,
                                enabled = operatingMode == OperatingMode.OFFLINE,
                                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                modifier = Modifier.fillMaxWidth().padding(8.dp)
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ServerControls(operatingMode, isPublishing, isListening, { viewModel.setPublishing(it) }, { 
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
                                    onThresholdSelected = { threshold -> viewModel.showStateAssignmentDialog(threshold) }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) })
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Button(onClick = { navigator.push(EyeContactScreen(activePuppet, serverIpAddress, viewModel::updateEyeState, viewModel::getImageData)) }) {
                                Text("Eye Contact")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.fillMaxHeight().weight(1f - (2 * panelWeight)))

                    Surface(
                        modifier = Modifier.fillMaxHeight().weight(panelWeight),
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
                                        selectedImage = uiState.selectedImage,
                                        selectedImageName = uiState.selectedImageName,
                                        selectedBlinkImage = uiState.selectedBlinkImage,
                                        selectedBlinkImageName = uiState.selectedBlinkImageName,
                                        newStateName = uiState.newStateName,
                                        onStateChange = viewModel::onStateCreationChange
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
                                            onBlinkRateChanged = { newBlinkRate -> viewModel.updateBlinkRate(state, newBlinkRate) },
                                            onApplyEffect = { effect -> viewModel.updateAppliedEffect(state, effect) }
                                        )
                                    }
                                }
                                item { HorizontalDivider() }
                                item {
                                    SpecialEffectsUI(
                                        specialEffectsManager = currentTroupe.specialEffectsManager,
                                        onSpecialEffectsManagerChanged = viewModel::onSpecialEffectsManagerChanged,
                                        onSaveEffect = { viewModel.onSpecialEffectUpdated() },
                                        activePuppet = currentPuppet,
                                        uploadsDir = viewModel.uploadsDir,
                                        preserveState = uiState.preserveState,
                                        onPreserveStateChanged = viewModel::onPreserveStateChanged
                                    )
                                }
                            } else {
                                item {
                                    Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
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
                        PuppetControls(troupe, activePuppet, viewModel::setActivePuppet, viewModel::createNewPuppet)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ModeControls(operatingMode, viewModel::setOperatingMode)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        TextField(
                            value = serverIpAddress,
                            onValueChange = viewModel::onServerIpAddressChanged,
                            label = { Text("Server IP Address") },
                            singleLine = true,
                            enabled = operatingMode == OperatingMode.OFFLINE,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth().padding(8.dp)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        ServerControls(operatingMode, isPublishing, isListening, { viewModel.setPublishing(it) }, { 
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
                                onThresholdSelected = { threshold -> viewModel.showStateAssignmentDialog(threshold) }
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
                                    selectedImage = uiState.selectedImage,
                                    selectedImageName = uiState.selectedImageName,
                                    selectedBlinkImage = uiState.selectedBlinkImage,
                                    selectedBlinkImageName = uiState.selectedBlinkImageName,
                                    newStateName = uiState.newStateName,
                                    onStateChange = viewModel::onStateCreationChange
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
                                        onBlinkRateChanged = { newBlinkRate -> viewModel.updateBlinkRate(state, newBlinkRate) },
                                        onApplyEffect = { effectName -> viewModel.updateAppliedEffect(state, effectName) }
                                    )
                                }
                            }
                            item { HorizontalDivider() }
                            item {
                                SpecialEffectsUI(
                                    specialEffectsManager = currentTroupe.specialEffectsManager,
                                    onSpecialEffectsManagerChanged = viewModel::onSpecialEffectsManagerChanged,
                                    onSaveEffect = { viewModel.onSpecialEffectUpdated() },
                                    activePuppet = currentPuppet,
                                    uploadsDir = viewModel.uploadsDir,
                                    preserveState = uiState.preserveState,
                                    onPreserveStateChanged = viewModel::onPreserveStateChanged
                                )
                            }
                        } else {
                            item {
                                Box(modifier = Modifier.fillParentMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
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
                    Button(onClick = { navigator.push(EyeContactScreen(activePuppet, serverIpAddress, viewModel::updateEyeState, viewModel::getImageData)) }) { Text("Eye Contact") }
                }
            }
        }
    }
}

@Composable
expect fun ByteArray.toImageBitmap(): ImageBitmap
