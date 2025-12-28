package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.center
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toOffset
import org.menagerie.puppet_master.state_machine.Handle
import org.menagerie.puppet_master.state_machine.Node
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.toSerializableOffset
import org.menagerie.puppet_master.toSize

@Composable
fun NodeView(
    node: Node,
    title: String,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates,
    expanded: Boolean = false,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val size = if (expanded) node.expandedSize?.toSize() ?: node.size.toSize() else node.size.toSize()
    val widthInDp = with(density) { size.width.toDp() }
    val heightInDp = with(density) { size.height.toDp() }

    Box(modifier = Modifier.width(widthInDp).height(heightInDp)) {
        Card(modifier = Modifier
            .padding(8.dp)
            .pointerInput(node.id) {
                detectTapGestures(onLongPress = {
                    editorViewModel.deleteNode(node.id)
                })
            }
        ) {
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
                    Text(title, fontWeight = FontWeight.Bold)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    // Input Handles
                    if (node.inputs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                node.inputs.forEach { handle ->
                                    HandleView(node.id, handle, editorViewModel, canvasCoordinates)
                                }
                            }
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        content()
                    }
                    // Output Handles
                    if (node.outputs.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.SpaceEvenly
                            ) {
                                node.outputs.forEach { handle ->
                                    HandleView(node.id, handle, editorViewModel, canvasCoordinates)
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
fun HandleView(
    nodeId: String,
    handle: Handle,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates
) {
    val handleId = handle.id
    Canvas(modifier = Modifier
        .size(20.dp)
        .onGloballyPositioned { coordinates ->
            val handlePositionInCanvas = canvasCoordinates.windowToLocal(
                coordinates.localToWindow(Offset.Zero)
            ) + coordinates.size.center.toOffset()
            editorViewModel.updateHandlePosition(nodeId, handleId, handlePositionInCanvas)
        }
        .pointerInput(nodeId, handleId) {
            detectDragGestures(
                onDragStart = { editorViewModel.onWireDragStart(nodeId, handleId) },
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
