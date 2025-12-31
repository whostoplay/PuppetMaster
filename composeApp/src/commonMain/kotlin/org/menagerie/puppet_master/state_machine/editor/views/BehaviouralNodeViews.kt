package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.ActiveSpecialEffect
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.controls.ColorGrid
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.state_machine.GoThroughStateNode
import org.menagerie.puppet_master.state_machine.WithEffectNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import kotlin.math.roundToLong

@Composable
fun GoThroughStateNodeView(
    node: GoThroughStateNode,
    puppetState: PuppetStateInfo?,
    puppetStates: List<PuppetStateInfo>,
    onStateNameChanged: (String) -> Unit,
    idleImage: ImageBitmap,
    editorViewModel: NodeEditorViewModel,
    uploadsDir: String,
    canvasCoordinates: LayoutCoordinates
) {
    var expanded by remember { mutableStateOf(false) }

    NodeView(
        node = node,
        title = "Go Through State",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates,
        expanded = expanded
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            if (node.puppetId == null) {
                Text("Awaiting Puppet Context", color = Color.White)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Set State: ", fontWeight = FontWeight.Bold)
                    Box(modifier = Modifier.weight(1f)) {
                        Box {
                            Row(
                                modifier = Modifier.clickable { expanded = true },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(node.stateName.ifEmpty { "Select State" })
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                puppetStates.forEach { state ->
                                    DropdownMenuItem(
                                        text = { Text(state.name) },
                                        onClick = {
                                            onStateNameChanged(state.name)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    LivePreview(
                        operatingMode = OperatingMode.OFFLINE,
                        puppetState = puppetState,
                        isBlinking = false,
                        uploadsDir = uploadsDir,
                        backgroundColor = Color.Transparent,
                        serverIp = "",
                        activeSpecialEffect = null,
                        isAudienceCheckForced = false,
                        window = null,
                        displayedImageName = puppetState?.imageName,
                        idleImage = idleImage,
                        onFocusPointUpdate = {}
                    )
                }

                // Delay Slider
                Text("Delay: %.2f s".format(node.delay / 1000f))
                Slider(
                    value = node.delay.toFloat(),
                    onValueChange = {
                        editorViewModel.updateNode(node.copy(delay = it.roundToLong()))
                    },
                    valueRange = 0f..10000f // 0 to 10 seconds
                )
            }
        }
    }
}

@Composable
fun WithEffectNodeView(
    node: WithEffectNode,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates,
    puppetState: PuppetStateInfo?,
    idleImage: ImageBitmap,
    uploadsDir: String
) {
    var expanded by remember { mutableStateOf(false) }
    var activePreviewEffect by remember { mutableStateOf<ActiveSpecialEffect?>(null) }
    var showGlowColorPicker by remember { mutableStateOf(false) }

    if (showGlowColorPicker) {
        AlertDialog(
            modifier = Modifier.size(300.dp),
            onDismissRequest = { showGlowColorPicker = false },
            text = { Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { ColorGrid{ color ->
                editorViewModel.updateNode(node.copy(effect = node.effect.copy(glowColor = color.toArgb())))
                showGlowColorPicker = false
            } } },
            confirmButton = { }
        )
    }

    LaunchedEffect(node.effect) {
        activePreviewEffect = activePreviewEffect?.copyWithPreservedStartTime(node.effect)
            ?: ActiveSpecialEffect(node.effect)
    }

    NodeView(
        node = node,
        title = "With Effect",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates,
        expanded = expanded
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            if (expanded) {
                // Vibration
                Text("Vibration Distance: ${node.effect.vibrationDistance}")
                Slider(
                    value = node.effect.vibrationDistance,
                    onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(vibrationDistance = it))) },
                    valueRange = 0f..1f
                )
                Text("Vibration Speed: ${node.effect.vibrationSpeed}")
                Slider(
                    value = node.effect.vibrationSpeed,
                    onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(vibrationSpeed = it))) },
                    valueRange = 0f..1f
                )

                // Glow
                node.effect.glowIntensity?.let {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(Color(node.effect.glowColor ?: 0xFFFFFFFF.toInt()))
                                .clickable { showGlowColorPicker = true }
                        )
                        Spacer(modifier = Modifier.weight(.125f))
                        Text("Glow Intensity: $it")
                    }
                    Slider(
                        value = it,
                        onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(glowIntensity = it))) },
                        valueRange = 0f..6f
                    )
                }

                // Scale
                Text("Scale X: ${node.effect.scaleX}")
                Slider(
                    value = node.effect.scaleX,
                    onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(scaleX = it))) },
                    valueRange = 0.25f..2f
                )
                Text("Scale Y: ${node.effect.scaleY}")
                Slider(
                    value = node.effect.scaleY,
                    onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(scaleY = it))) },
                    valueRange = 0.25f..2f
                )
                Text("Scale Speed: ${node.effect.scaleSpeed}")
                Slider(
                    value = node.effect.scaleSpeed,
                    onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(scaleSpeed = it))) },
                    valueRange = 0f..1f
                )

                // Spin
                Text("Spin Speed: ${node.effect.spinSpeed}")
                Slider(
                    value = node.effect.spinSpeed,
                    onValueChange = { editorViewModel.updateNode(node.copy(effect = node.effect.copy(spinSpeed = it))) },
                    valueRange = 0f..20f
                )
            } else {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    if (puppetState != null) {
                        LivePreview(
                            operatingMode = OperatingMode.OFFLINE,
                            puppetState = puppetState,
                            isBlinking = false,
                            uploadsDir = uploadsDir,
                            backgroundColor = Color.Transparent,
                            serverIp = "",
                            activeSpecialEffect = activePreviewEffect,
                            isAudienceCheckForced = false,
                            window = null,
                            displayedImageName = puppetState.imageName,
                            idleImage = idleImage,
                            onFocusPointUpdate = {}
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { expanded = !expanded }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = "Expand")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
