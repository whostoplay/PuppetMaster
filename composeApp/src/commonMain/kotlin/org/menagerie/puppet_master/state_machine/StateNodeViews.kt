package org.menagerie.puppet_master.state_machine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.AnimationState
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.previews.LivePreview
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.toSerializableOffset
import org.menagerie.puppet_master.toSize

@Composable
fun SetStateNodeView(
    node: SetStateNode,
    isStartNode: Boolean,
    puppetState: PuppetStateInfo?,
    puppetStates: List<PuppetStateInfo>,
    onStateNameChanged: (String) -> Unit,
    idleImage: ImageBitmap,
    editorViewModel: NodeEditorViewModel,
    uploadsDir: String
) {
    val density = LocalDensity.current
    val size = node.size.toSize()
    val widthInDp = with(density) { size.width.toDp() }
    val heightInDp = with(density) { size.height.toDp() }

    Box(modifier = Modifier.width(widthInDp).height(heightInDp)) {
        Card(modifier = Modifier.padding(8.dp)) {
            Column {
                val dragLabel = if (isStartNode) "Start Node" else "Set State"
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.DarkGray)
                        .pointerInput(node.id) {
                            detectDragGestures(
                                onDragStart = { editorViewModel.onNodeDragStart(node.id) },
                                onDragEnd = { editorViewModel.onNodeDragEnd() },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    editorViewModel.onNodeDrag(dragAmount.toSerializableOffset())
                                }
                            )
                        }
                        .padding(4.dp)
                ) {
                    Text(dragLabel, fontWeight = FontWeight.Bold)
                }
                Row {
                    // Input Handles
                    if (!isStartNode) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                node.inputs.forEach { handle ->
                                    HandleView(node.id, handle, editorViewModel)
                                }
                            }
                        }
                    }
                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        var expanded by remember { mutableStateOf(false) }
                        val label = if (isStartNode) "Start: " else "Set State: "

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, fontWeight = FontWeight.Bold)
                            Box(modifier = Modifier.weight(1f)) {
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
                                    DropdownMenuItem(
                                        text = { Text(ANY_STATE) },
                                        onClick = {
                                            onStateNameChanged(ANY_STATE)
                                            expanded = false
                                        }
                                    )
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
                    }
                    // Output Handles
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxHeight(),
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            node.outputs.forEach { handle ->
                                HandleView(node.id, handle, editorViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandleView(nodeId: String, handle: Handle, editorViewModel: NodeEditorViewModel) {
    val handleId = handle.id
    Canvas(modifier = Modifier
        .size(20.dp)
        .onGloballyPositioned { coordinates ->
            editorViewModel.updateHandlePosition(nodeId, handleId, coordinates.boundsInRoot().center)
        }
        .pointerInput(nodeId, handleId) {
            detectDragGestures(
                onDragStart = { offset -> editorViewModel.onWireDragStart(nodeId, handleId) },
                onDragEnd = { editorViewModel.onWireDragEnd() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    editorViewModel.onWireDrag(dragAmount)
                }
            )
        }) { // Increased size for easier tapping
        drawCircle(
            color = Color.White,
            radius = size.minDimension / 2 - 2, // 2px padding
        )
        drawCircle(
            color = Color.Black,
            radius = size.minDimension / 2 - 4,
        )
    }
}
