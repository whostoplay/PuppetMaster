package org.menagerie.puppet_master.state_machine

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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

@Composable
fun SetStateNodeView(
    node: SetStateNode,
    isStartNode: Boolean,
    puppetState: PuppetStateInfo?,
    idleImage: ImageBitmap,
    editorViewModel: NodeEditorViewModel,
    uploadsDir: String
) {
    val density = LocalDensity.current
    val widthInDp = with(density) { node.size.width.toDp() }
    val heightInDp = with(density) { node.size.height.toDp() }

    Box(modifier = Modifier.width(widthInDp).height(heightInDp)) {
        Card(modifier = Modifier.padding(8.dp)) {
            Row {
                // Input Handles
                if (!isStartNode) {
                    Row(
                        modifier = Modifier.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier,
                            verticalArrangement = Arrangement.SpaceEvenly
                        ) {
                            node.inputs.forEach { handle ->
                                HandleView(node.id, handle.id, editorViewModel)
                            }
                        }
                    }
                }
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isStartNode) {
                        Text("Start: ${node.stateName}", fontWeight = FontWeight.Bold)
                    } else {
                        Text("Set State: ${node.stateName}", fontWeight = FontWeight.Bold)
                    }
                    Box(modifier = Modifier.width(150.dp).height(150.dp)) {
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
                        modifier = Modifier,
                        verticalArrangement = Arrangement.SpaceEvenly
                    ) {
                        node.outputs.forEach { handle ->
                            HandleView(node.id, handle.id, editorViewModel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HandleView(nodeId: String, handleId: String, editorViewModel: NodeEditorViewModel) {
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
