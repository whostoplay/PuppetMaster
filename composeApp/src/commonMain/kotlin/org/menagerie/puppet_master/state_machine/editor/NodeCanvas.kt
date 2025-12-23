package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntOffset
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.state_machine.ConditionalNode
import org.menagerie.puppet_master.state_machine.HotKeyNode
import org.menagerie.puppet_master.state_machine.HotKeyNodeView
import org.menagerie.puppet_master.state_machine.NodeGraph
import org.menagerie.puppet_master.state_machine.SetStateNode
import org.menagerie.puppet_master.state_machine.SetStateNodeView
import org.menagerie.puppet_master.state_machine.StateNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNodeView
import org.menagerie.puppet_master.toOffset
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeCanvas(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    editorViewModel: NodeEditorViewModel
) {
    val graph by editorViewModel.nodeGraph.collectAsState()
    val handlePositions by editorViewModel.handlePositions.collectAsState()
    val wireDragInfo by editorViewModel.wireDragInfo.collectAsState()
    val draggedWireEndPosition by editorViewModel.draggedWireEndPosition.collectAsState()
    var canvasCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { canvasCoordinates = it }
                .pointerInput(graph.wires) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val wire = findWireAt(offset, graph.wires, handlePositions, canvasCoordinates)
                            if (wire != null) {
                                editorViewModel.deleteWire(wire)
                            }
                        }
                    )
                }
        ) {
            graph.wires.forEach { wire ->
                val fromPosAbsolute = handlePositions["${wire.fromNodeId}-${wire.fromHandleId}"]
                val toPosAbsolute = handlePositions["${wire.toNodeId}-${wire.toHandleId}"]

                if (fromPosAbsolute != null && toPosAbsolute != null) {
                    canvasCoordinates?.let {
                        val fromPosLocal = fromPosAbsolute - it.localToRoot(Offset.Zero)
                        val toPosLocal = toPosAbsolute - it.localToRoot(Offset.Zero)
                        drawLine(
                            color = Color.Gray,
                            start = fromPosLocal,
                            end = toPosLocal,
                            strokeWidth = 2f
                        )
                    }
                }
            }

            wireDragInfo?.let { dragInfo ->
                draggedWireEndPosition?.let { endPos ->
                    val startPos = handlePositions["${dragInfo.fromNodeId}-${dragInfo.fromHandleId}"]
                    if (startPos != null) {
                        canvasCoordinates?.let { coords ->
                            val startPosLocal = startPos - coords.localToRoot(Offset.Zero)
                            val endPosLocal = endPos - coords.localToRoot(Offset.Zero)
                            drawLine(
                                color = Color.White,
                                start = startPosLocal,
                                end = endPosLocal,
                                strokeWidth = 2f
                            )
                        }
                    }
                }
            }
        }

        graph.nodes.values.forEach { node ->
            ContextMenuArea(items = {
                listOf(
                    ContextMenuItem("Delete") {
                        editorViewModel.deleteNode(node.id)
                    }
                )
            }) {
                Box(
                    modifier = Modifier
                        .offset {
                            val offset = node.position.toOffset()
                            IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                        }
                ) {
                    when (node) {
                        is StateNode -> RenderStateNode(node, mainViewModel, editorViewModel, graph)
                        is ConditionalNode -> RenderConditionalNode(node, editorViewModel)
                    }
                }
            }
        }
    }
}

private fun findWireAt(
    position: Offset,
    wires: List<org.menagerie.puppet_master.state_machine.Wire>,
    handlePositions: Map<String, Offset>,
    canvasCoordinates: LayoutCoordinates?
): org.menagerie.puppet_master.state_machine.Wire? {
    if (canvasCoordinates == null) return null

    return wires.find { wire ->
        val fromPos = handlePositions["${wire.fromNodeId}-${wire.fromHandleId}"]?.let { it - canvasCoordinates.localToRoot(Offset.Zero) }
        val toPos = handlePositions["${wire.toNodeId}-${wire.toHandleId}"]?.let { it - canvasCoordinates.localToRoot(Offset.Zero) }

        if (fromPos != null && toPos != null) {
            val dist = distanceToSegment(position, fromPos, toPos)
            dist < 10f // 10px tolerance
        } else {
            false
        }
    }
}

private fun distanceToSegment(p: Offset, v: Offset, w: Offset): Float {
    val l2 = (v - w).getDistanceSquared()
    if (l2 == 0.0f) return (p - v).getDistance()
    val t = ((p.x - v.x) * (w.x - v.x) + (p.y - v.y) * (w.y - v.y)) / l2
    val tClamped = t.coerceIn(0f, 1f)
    val projection = v + (w - v) * tClamped
    return (p - projection).getDistance()
}


@Composable
private fun RenderStateNode(node: StateNode, mainViewModel: MainViewModel, editorViewModel: NodeEditorViewModel, graph: NodeGraph) {
    val puppetStates by editorViewModel.mainViewModel.puppetStates.collectAsState()
    val idleImage by editorViewModel.mainViewModel.idleImage.collectAsState()

    if (node is SetStateNode) {
        val stateInfo = puppetStates.find { it.name == node.stateName }
        idleImage?.let {
            SetStateNodeView(
                node = node,
                isStartNode = node.id == graph.startNodeId,
                puppetState = stateInfo,
                puppetStates = puppetStates,
                onStateNameChanged = { editorViewModel.updateNode(node.copy(stateName = it)) },
                idleImage = it,
                editorViewModel = editorViewModel,
                uploadsDir = mainViewModel.uploadsDir
            )
        }
    }
}

@Composable
private fun RenderConditionalNode(node: ConditionalNode, editorViewModel: NodeEditorViewModel) {
    when (node) {
        is VolumeThresholdNode -> {
            VolumeThresholdNodeView(
                node = node,
                onThresholdChanged = { newThreshold ->
                    editorViewModel.updateNode(node.copy(threshold = newThreshold))
                },
                editorViewModel = editorViewModel
            )
        }
        is HotKeyNode -> {
            HotKeyNodeView(
                node = node,
                onHotKeyChanged = { newHotKey ->
                    editorViewModel.updateNode(node.copy(hotKey = newHotKey))
                },
                editorViewModel = editorViewModel
            )
        }
    }
}
