package org.menagerie.puppet_master.state_machine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.controls.HotkeySelector
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.toSerializableOffset

@Composable
fun VolumeThresholdNodeView(
    node: VolumeThresholdNode,
    onThresholdChanged: (Float) -> Unit,
    editorViewModel: NodeEditorViewModel
) {
    var currentLevel by remember { mutableFloatStateOf(0f) } // This would be fed by a real audio stream
    val density = LocalDensity.current
    val widthInDp = with(density) { node.size.width.toDp() }
    val heightInDp = with(density) { node.size.height.toDp() }

    Box(modifier = Modifier.width(widthInDp).height(heightInDp)) {
        Card(modifier = Modifier.padding(8.dp)) {
            Column {
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
                    Text("Volume Threshold", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    // Input Handles
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

                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        VolumeIndicator(
                            level = currentLevel,
                            threshold = node.threshold,
                            onThresholdChange = onThresholdChanged,
                            modifier = Modifier.height(30.dp).fillMaxWidth()
                        )
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
fun HotKeyNodeView(
    node: HotKeyNode,
    onHotKeyChanged: (Hotkey) -> Unit,
    editorViewModel: NodeEditorViewModel
) {
    val density = LocalDensity.current
    val widthInDp = with(density) { node.size.width.toDp() }
    val heightInDp = with(density) { node.size.height.toDp() }

    Box(modifier = Modifier.width(widthInDp).height(heightInDp)) {
        Card(modifier = Modifier.padding(8.dp)) {
            Column {
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
                    Text("Hot Key", fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    // Input Handles
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

                    Column(modifier = Modifier.padding(16.dp).weight(1f)) {
                        HotkeySelector(
                            label = "Hotkey",
                            hotkey = node.hotkey,
                            onHotkeyChanged = onHotKeyChanged,
                        )
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
