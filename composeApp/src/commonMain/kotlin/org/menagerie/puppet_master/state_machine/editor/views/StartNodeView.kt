package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.state_machine.Handle
import org.menagerie.puppet_master.state_machine.StartNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.toSerializableOffset
import org.menagerie.puppet_master.toSize

@Composable
fun StartNodeView(
    node: StartNode,
    editorViewModel: NodeEditorViewModel
) {
    val density = LocalDensity.current
    val size = node.size.toSize()
    val widthInDp = with(density) { size.width.toDp() }
    val heightInDp = with(density) { size.height.toDp() }

    Box(modifier = Modifier.width(widthInDp).height(heightInDp)) {
        Card(modifier = Modifier.padding(8.dp)) {
            Column {
                // Title bar that is also the drag handle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primary)
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
                    Text("Start Node", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }

                // Node content
                Row(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("START")
                    node.outputs.forEach { handle ->
                        HandleView(node.id, handle, editorViewModel)
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
                onDragStart = { editorViewModel.onWireDragStart(nodeId, handleId) },
                onDragEnd = { editorViewModel.onWireDragEnd() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    editorViewModel.onWireDrag(dragAmount)
                }
            )
        }) {
        drawCircle(
            color = Color.White,
            radius = size.minDimension / 2 - 2,
        )
        drawCircle(
            color = Color.Black,
            radius = size.minDimension / 2 - 4,
        )
    }
}
