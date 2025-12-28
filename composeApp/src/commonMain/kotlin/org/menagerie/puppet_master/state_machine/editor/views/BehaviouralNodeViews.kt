package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.AnimationState
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.state_machine.GoThroughStateNode
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
    NodeView(
        node = node,
        title = "Go Through State",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            if (node.puppetId == null) {
                Text("Awaiting Puppet Context", color = Color.White)
            } else {
                var expanded by remember { mutableStateOf(false) }

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
                        animationState = AnimationState(),
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
