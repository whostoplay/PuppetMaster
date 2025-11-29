package org.menagerie.puppet_master

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
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
    val selectedState by viewModel.selectedState.collectAsState()

    var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    var selectedBlinkImage by remember { mutableStateOf<ByteArray?>(null) }
    var selectedBlinkImageName by remember { mutableStateOf("") }
    var newStateName by remember { mutableStateOf("") }
    var backgroundColor by remember { mutableStateOf(Color.Green) }

    var showControls by remember { mutableStateOf(true) }
    val isHoveringOn = remember { mutableStateMapOf<String, Boolean>() }
    val isHoveringOnControls = isHoveringOn.values.any { it }
    val controlsAlpha by animateFloatAsState(if (showControls) 1f else 0f)

    LaunchedEffect(showControls, isHoveringOnControls) {
        if (showControls && !isHoveringOnControls) {
            delay(1500)
            if (!isHoveringOnControls) {
                showControls = false
            }
        }
    }

    MaterialTheme {
        BoxWithConstraints(modifier = Modifier.fillMaxSize().onPointerEvent(PointerEventType.Move) { showControls = true }) {
            val isLandscape = maxWidth > maxHeight

            LivePreview(operatingMode, displayedImageName, viewModel.uploadsDir, backgroundColor)

            Box(modifier = Modifier.graphicsLayer(alpha = controlsAlpha).fillMaxSize()) {
                if (isLandscape) {
                    Row(modifier = Modifier.fillMaxSize(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(
                            modifier = Modifier.fillMaxHeight().weight(0.25f)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            PuppetControls(troupe, activePuppet, viewModel::setActivePuppet, viewModel::createNewPuppet) { isHoveringOn["puppet"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ModeControls(operatingMode, viewModel::setOperatingMode) { isHoveringOn["mode"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ServerControls(operatingMode, isPublishing, isListening, viewModel::setPublishing, viewModel::toggleListening) { isHoveringOn["server"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ColorPicker(onColorSelected = { backgroundColor = it }) { isHoveringOn["color"] = it }
                        }

                        Spacer(modifier = Modifier.fillMaxHeight().weight(0.5f))

                        Column(
                            modifier = Modifier.fillMaxHeight().weight(0.25f)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                        ) {
                            if (activePuppet != null) {
                                StateCreation(
                                    modifier = Modifier.fillMaxWidth(),
                                    viewModel = viewModel,
                                    selectedImage = selectedImage,
                                    selectedImageName = selectedImageName,
                                    selectedBlinkImage = selectedBlinkImage,
                                    selectedBlinkImageName = selectedBlinkImageName,
                                    newStateName = newStateName,
                                    onStateChange = { si, sin, sbi, sbin, nsn ->
                                        selectedImage = si
                                        selectedImageName = sin
                                        selectedBlinkImage = sbi
                                        selectedBlinkImageName = sbin
                                        newStateName = nsn
                                    },
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
                } else {
                    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                        Column(
                            modifier = Modifier.fillMaxWidth().weight(0.25f)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                                .verticalScroll(rememberScrollState())
                        ) {
                            PuppetControls(troupe, activePuppet, viewModel::setActivePuppet, viewModel::createNewPuppet) { isHoveringOn["puppet"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ModeControls(operatingMode, viewModel::setOperatingMode) { isHoveringOn["mode"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ServerControls(operatingMode, isPublishing, isListening, viewModel::setPublishing, viewModel::toggleListening) { isHoveringOn["server"] = it }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ColorPicker(onColorSelected = { backgroundColor = it }) { isHoveringOn["color"] = it }
                        }

                        Spacer(modifier = Modifier.fillMaxWidth().weight(0.5f))

                        Column(
                            modifier = Modifier.fillMaxWidth().weight(0.25f)
                                .background(MaterialTheme.colorScheme.background.copy(alpha = 0.8f))
                        ) {
                            if (activePuppet != null) {
                                StateCreation(
                                    modifier = Modifier.fillMaxWidth(),
                                    viewModel = viewModel,
                                    selectedImage = selectedImage,
                                    selectedImageName = selectedImageName,
                                    selectedBlinkImage = selectedBlinkImage,
                                    selectedBlinkImageName = selectedBlinkImageName,
                                    newStateName = newStateName,
                                    onStateChange = { si, sin, sbi, sbin, nsn ->
                                        selectedImage = si
                                        selectedImageName = sin
                                        selectedBlinkImage = sbi
                                        selectedBlinkImageName = sbin
                                        newStateName = nsn
                                    },
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
