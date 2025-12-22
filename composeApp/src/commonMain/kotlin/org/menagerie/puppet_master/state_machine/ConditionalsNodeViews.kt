package org.menagerie.puppet_master.state_machine

import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel

@Composable
fun VolumeThresholdNodeView(
    node: VolumeThresholdNode, 
    onThresholdChanged: (Float) -> Unit,
    editorViewModel: NodeEditorViewModel
) {
    var currentLevel by remember { mutableStateOf(0f) } // This would be fed by a real audio stream

    Box {
        Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Volume Threshold")
                VolumeIndicator(
                    level = currentLevel,
                    threshold = node.threshold,
                    onThresholdChange = onThresholdChanged,
                    modifier = Modifier.height(30.dp).fillMaxWidth()
                )
            }
        }

        // Input Handles
        Row(modifier = Modifier.align(Alignment.CenterStart).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                node.inputs.forEach { handle ->
                    HandleView(node.id, handle.id, editorViewModel)
                }
            }
        }

        // Output Handles
        Row(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.fillMaxHeight(), verticalArrangement = Arrangement.SpaceEvenly) {
                node.outputs.forEach { handle ->
                    HandleView(node.id, handle.id, editorViewModel)
                }
            }
        }
    }
}

@Composable
private fun HandleView(nodeId: String, handleId: String, editorViewModel: NodeEditorViewModel) {
    Canvas(modifier = Modifier.size(20.dp).pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { editorViewModel.onWireDragStart(nodeId, handleId) },
            onDragEnd = { editorViewModel.onWireDragEnd() },
            onDrag = { change, _ -> change.consume() }
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
