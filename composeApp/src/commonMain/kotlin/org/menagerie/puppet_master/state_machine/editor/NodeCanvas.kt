package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.state_machine.*
import org.menagerie.puppet_master.state_machine.editor.views.DelayTimerNodeView
import org.menagerie.puppet_master.state_machine.editor.views.GoThroughStateNodeView
import org.menagerie.puppet_master.state_machine.editor.views.HotKeyNodeView
import org.menagerie.puppet_master.state_machine.editor.views.ResetSetNodeView
import org.menagerie.puppet_master.state_machine.editor.views.SetPuppetNodeView
import org.menagerie.puppet_master.state_machine.editor.views.SetStateNodeView
import org.menagerie.puppet_master.state_machine.editor.views.StartNodeView
import org.menagerie.puppet_master.state_machine.editor.views.VolumeThresholdNodeView
import org.menagerie.puppet_master.toOffset
import kotlin.math.roundToInt

data class HighlightInfo(val color: Color, val number: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeCanvas(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    editorViewModel: NodeEditorViewModel,
    highlightMode: Boolean
) {
    val graph by editorViewModel.nodeGraph.collectAsState()
    val handlePositions by editorViewModel.handlePositions.collectAsState()
    val wireDragInfo by editorViewModel.wireDragInfo.collectAsState()
    val draggedWireEndPosition by editorViewModel.draggedWireEndPosition.collectAsState()
    var canvasCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }

    val highlightData by remember(graph, highlightMode) {
        mutableStateOf(if (highlightMode) calculateHighlightInfo(graph) else emptyMap())
    }

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .onSizeChanged { editorViewModel.updateCanvasSize(it) }
                .onGloballyPositioned { canvasCoordinates = it }
                .pointerInput(graph.wires) {
                    detectTapGestures(
                        onLongPress = { offset ->
                            val wire =
                                findWireAt(offset, graph.wires, handlePositions, canvasCoordinates)
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
                    val startPos =
                        handlePositions["${dragInfo.fromNodeId}-${dragInfo.fromHandleId}"]
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

        val childrenOfStart = remember(graph.nodes) {
            val children = graph.startNodeId?.let { startId ->
                graph.wires.filter { it.fromNodeId == startId }
                    .mapNotNull { graph.nodes[it.toNodeId] }
            } ?: emptyList()
            Pair(children, children.map { it.branchPriority })
        }.first


        graph.nodes.values.forEach { node ->
            val highlightInfo = highlightData[node.id]

            Box(
                modifier = Modifier
                    .offset {
                        val offset = node.position.toOffset()
                        IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                    }
            ) {
                val content: @Composable () -> Unit = {
                    when (node) {
                        is StateNode -> RenderStateNode(node, mainViewModel, editorViewModel)
                        is ConditionalNode -> RenderConditionalNode(node, editorViewModel)
                        is BehaviouralNode -> RenderBehaviouralNode(node, mainViewModel, editorViewModel)
                        is StartNode -> StartNodeView(node, editorViewModel)
                        is UtilityNode -> RenderUtilityNode(node, editorViewModel)
                    }
                }

                if (highlightMode && highlightInfo != null) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = highlightInfo.color.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = highlightInfo.color,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp)
                    ) {
                        content()
                    }

                    val isChildOfStart = childrenOfStart.any { it.id == node.id }
                    if (isChildOfStart) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 12.dp, y = (-12).dp)
                        ) {
                            BranchPriorityDropdown(node, childrenOfStart, editorViewModel)
                        }
                    } else {
                        Text(
                            text = highlightInfo.number.toString(),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 12.dp, y = (-12).dp),
                            color = Color.White,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        )
                    }
                } else {
                    content()
                }
            }
        }
    }
}

@Composable
private fun BranchPriorityDropdown(
    node: Node,
    childrenOfStart: List<Node>,
    editorViewModel: NodeEditorViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedChildren = childrenOfStart.sortedBy { it.branchPriority }


    val items = (1..sortedChildren.size).toList()
    val currentNodeIndex = sortedChildren.indexOfFirst { it.id == node.id }

    Box {
        Row(
            modifier = Modifier.clickable { expanded = true },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = (currentNodeIndex + 1).toString(),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Icon(Icons.Default.ArrowDropDown, contentDescription = "Change priority", tint = Color.White)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            items.forEachIndexed { newIndex, priorityNum ->
                if (newIndex != currentNodeIndex) {
                    DropdownMenuItem(
                        text = { Text(priorityNum.toString()) },
                        onClick = {
                            val nodeToSwapWith = sortedChildren[newIndex]

                            editorViewModel.swapNodePriorities(node.id, nodeToSwapWith.id)

                            expanded = false
                        }
                    )
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
private fun RenderStateNode(node: StateNode, mainViewModel: MainViewModel, editorViewModel: NodeEditorViewModel) {
    val troupe by editorViewModel.mainViewModel.troupe.collectAsState()
    val puppets = troupe?.puppets ?: emptyList()
    val idleImage by editorViewModel.mainViewModel.idleImage.collectAsState()

    if (node is SetStateNode) {
        val puppet = puppets.find { it.name == node.puppetId }
        val puppetStates = puppet?.states ?: emptyList()
        val stateInfo = puppetStates.find { it.name == node.stateName }

        idleImage?.let { bitmap ->
            SetStateNodeView(
                node = node,
                puppetState = stateInfo,
                puppetStates = puppetStates,
                onStateNameChanged = { editorViewModel.updateNode(node.copy(stateName = it)) },
                idleImage = bitmap,
                editorViewModel = editorViewModel,
                uploadsDir = mainViewModel.uploadsDir
            )
        }
    }
}

@Composable
private fun RenderBehaviouralNode(
    node: BehaviouralNode,
    mainViewModel: MainViewModel,
    editorViewModel: NodeEditorViewModel
) {
    val troupe by mainViewModel.troupe.collectAsState()
    val puppets = troupe?.puppets ?: emptyList()
    val idleImage by mainViewModel.idleImage.collectAsState()

    when (node) {
        is GoThroughStateNode -> {
            val puppet = puppets.find { it.name == node.puppetId }
            val puppetStates = puppet?.states ?: emptyList()
            val stateInfo = puppetStates.find { it.name == node.stateName }

            idleImage?.let { bitmap ->
                GoThroughStateNodeView(
                    node = node,
                    puppetState = stateInfo,
                    puppetStates = puppetStates,
                    onStateNameChanged = { editorViewModel.updateNode(node.copy(stateName = it)) },
                    idleImage = bitmap,
                    editorViewModel = editorViewModel,
                    uploadsDir = mainViewModel.uploadsDir
                )
            }
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
                    editorViewModel.updateNode(node.copy(hotkey = newHotKey, mode = newHotKey.hold))
                },
                editorViewModel = editorViewModel
            )
        }
    }
}

@Composable
private fun RenderUtilityNode(node: UtilityNode, editorViewModel: NodeEditorViewModel) {
    when (node) {
        is SetPuppetNode -> {
            SetPuppetNodeView(
                node = node,
                editorViewModel = editorViewModel
            )
        }
        is ResetSetNode -> {
            ResetSetNodeView(
                node = node,
                editorViewModel = editorViewModel
            )
        }
        is DelayTimerNode -> {
            DelayTimerNodeView(
                node = node,
                editorViewModel = editorViewModel
            )
        }
    }
}

private fun calculateHighlightInfo(graph: NodeGraph): Map<String, HighlightInfo> {
    val highlights = mutableMapOf<String, HighlightInfo>()
    val startNodeId = graph.startNodeId ?: return emptyMap()

    val colors = listOf(
        Color.Red,
        Color.Blue,
        Color.Green,
        Color.Cyan,
        Color.Magenta,
        Color.Yellow,
        Color(0xFFFFA500), // Orange
        Color(0xFF800080), // Purple
        Color(0xFFA52A2A), // Brown
        Color(0xFFFFC0CB), // Pink
        Color.LightGray
    )

    val visited = mutableSetOf(startNodeId)

    val firstLevelChildren = graph.wires
        .filter { it.fromNodeId == startNodeId }
        .mapNotNull { graph.nodes[it.toNodeId] }
        .sortedBy { it.branchPriority }
        .map { it.id }

    firstLevelChildren.forEachIndexed { index, nodeId ->
        highlights[nodeId] = HighlightInfo(colors[0], index + 1)
        visited.add(nodeId)
    }

    var colorIndex = 1
    var parentsToProcess = firstLevelChildren

    while (parentsToProcess.isNotEmpty()) {
        val nextLevelParents = mutableListOf<String>()
        for (parentId in parentsToProcess) {
            val children = graph.wires.filter { it.fromNodeId == parentId }.map { it.toNodeId }.filter { it !in visited }
            if (children.isNotEmpty()) {
                val color = colors[colorIndex % colors.size]
                children.forEachIndexed { index, childId ->
                    highlights[childId] = HighlightInfo(color, index + 1)
                    visited.add(childId)
                    nextLevelParents.add(childId)
                }
                colorIndex++
            }
        }
        parentsToProcess = nextLevelParents
    }

    return highlights
}
