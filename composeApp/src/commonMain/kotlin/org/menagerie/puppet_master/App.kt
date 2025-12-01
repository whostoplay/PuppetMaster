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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.menagerie.puppet_master.controls.ColorPicker
import org.menagerie.puppet_master.controls.ModeControls
import org.menagerie.puppet_master.controls.PuppetControls
import org.menagerie.puppet_master.controls.ServerControls
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.states.StateCreation
import org.menagerie.puppet_master.states.StateEditor
import org.menagerie.puppet_master.states.StateListing

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
@Preview
fun App() {
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
    val focusManager = LocalFocusManager.current

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

    MaterialTheme {
        @OptIn(ExperimentalComposeUiApi::class)
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize()
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
            val panelWeight = if (isDesktop) 0.25f else 1 / 3f

            LivePreview(operatingMode, displayedImageName, viewModel.uploadsDir, uiState.backgroundColor, serverIpAddress)

            Box(modifier = Modifier.graphicsLayer(alpha = controlsAlpha).fillMaxSize()) {
                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(
                            modifier = Modifier.fillMaxHeight().weight(panelWeight)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            PuppetControls(troupe, activePuppet, viewModel::setActivePuppet, viewModel::createNewPuppet) { isHoveringOn["puppet"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ModeControls(operatingMode, viewModel::setOperatingMode) { isHoveringOn["mode"] = it }
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
                            ServerControls(operatingMode, isPublishing, isListening, viewModel::setPublishing, viewModel::toggleListening) { isHoveringOn["server"] = it }
                            if (isListening) {
                                VolumeIndicator(
                                    level = audioLevel,
                                    orientation = Orientation.Horizontal,
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
                            ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) }) { isHoveringOn["color"] = it }
                        }

                        Spacer(modifier = Modifier.fillMaxHeight().weight(1f - (2 * panelWeight)))

                        Column(
                            modifier = Modifier.fillMaxHeight().weight(panelWeight)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (activePuppet != null) {
                                StateCreation(
                                    modifier = Modifier.fillMaxWidth(),
                                    viewModel = viewModel,
                                    selectedImage = uiState.selectedImage,
                                    selectedImageName = uiState.selectedImageName,
                                    selectedBlinkImage = uiState.selectedBlinkImage,
                                    selectedBlinkImageName = uiState.selectedBlinkImageName,
                                    newStateName = uiState.newStateName,
                                    onStateChange = viewModel::onStateCreationChange,
                                    onHover = { isHoveringOn["stateCreation"] = it }
                                )
                                HorizontalDivider()
                                StateListing(modifier = Modifier.fillMaxWidth().weight(1f), activePuppet = activePuppet, selectedState = selectedState, onStateSelected = viewModel::selectState) { isHoveringOn["stateListing"] = it }
                                HorizontalDivider()
                                selectedState?.let {
                                    StateEditor(
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedState = it,
                                        onBlinkRateChanged = { newBlinkRate -> viewModel.updateBlinkRate(it, newBlinkRate) },
                                        onHover = { isHoveringOn["stateEditor"] = it }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Create or select a puppet to get started.")
                                }
                            }
                        }
                    }
                } else { // Portrait
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(panelWeight)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            PuppetControls(troupe, activePuppet, viewModel::setActivePuppet, viewModel::createNewPuppet) { isHoveringOn["puppet"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ModeControls(operatingMode, viewModel::setOperatingMode) { isHoveringOn["mode"] = it }
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
                            ServerControls(operatingMode, isPublishing, isListening, viewModel::setPublishing, viewModel::toggleListening) { isHoveringOn["server"] = it }
                            if (isListening) {
                                VolumeIndicator(
                                    level = audioLevel,
                                    orientation = Orientation.Vertical,
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
                            ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) }) { isHoveringOn["color"] = it }
                        }

                        Spacer(modifier = Modifier.fillMaxWidth().weight(1f - (2 * panelWeight)))

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(panelWeight)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            if (activePuppet != null) {
                                StateCreation(
                                    modifier = Modifier.fillMaxWidth(),
                                    viewModel = viewModel,
                                    selectedImage = uiState.selectedImage,
                                    selectedImageName = uiState.selectedImageName,
                                    selectedBlinkImage = uiState.selectedBlinkImage,
                                    selectedBlinkImageName = uiState.selectedBlinkImageName,
                                    newStateName = uiState.newStateName,
                                    onStateChange = viewModel::onStateCreationChange,
                                    onHover = { isHoveringOn["stateCreation"] = it }
                                )
                                HorizontalDivider()
                                StateListing(modifier = Modifier.fillMaxWidth().weight(1f), activePuppet = activePuppet, selectedState = selectedState, onStateSelected = viewModel::selectState) { isHoveringOn["stateListing"] = it }
                                HorizontalDivider()
                                selectedState?.let {
                                    StateEditor(
                                        modifier = Modifier.fillMaxWidth(),
                                        selectedState = it,
                                        onBlinkRateChanged = { newBlinkRate -> viewModel.updateBlinkRate(it, newBlinkRate) },
                                        onHover = { isHoveringOn["stateEditor"] = it }
                                    )
                                }
                            } else {
                                Box(modifier = Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text("Create or select a puppet to get started.")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun ByteArray.toImageBitmap(): ImageBitmap
