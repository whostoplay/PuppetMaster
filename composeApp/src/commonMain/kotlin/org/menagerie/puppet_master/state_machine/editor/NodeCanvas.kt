package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.animation.core.*
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
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
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
import org.menagerie.puppet_master.decodeToImageBitmap
import org.menagerie.puppet_master.readFileAsByteArray
import org.menagerie.puppet_master.state_machine.*
import org.menagerie.puppet_master.state_machine.editor.views.*
import org.menagerie.puppet_master.toOffset
import kotlin.math.pow
import kotlin.math.roundToInt

data class HighlightInfo(val color: Color, val number: Int)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeCanvas(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    editorViewModel: NodeEditorViewModel,
    highlightMode: Boolean,
    isSimulating: Boolean
) {
    val graph by editorViewModel.nodeGraph.collectAsState()
    val topNodeId by editorViewModel.lastInteractedNodeId.collectAsState()
    val handlePositions by editorViewModel.handlePositions.collectAsState()
    val wireDragInfo by editorViewModel.wireDragInfo.collectAsState()
    val draggedWireEndPosition by editorViewModel.draggedWireEndPosition.collectAsState()
    var canvasCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
    var expandedNodes by remember { mutableStateOf(setOf<NodeId>()) }

    val activeNodes by editorViewModel.activeNodes.collectAsState()
    val activeWires by editorViewModel.activeWires.collectAsState()

    val infiniteTransition = rememberInfiniteTransition()
    val phase by infiniteTransition.animateFloat(
        initialValue = 40f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val highlightData by remember(graph, highlightMode) {
        mutableStateOf(if (highlightMode) calculateHighlightInfo(graph) else emptyMap())
    }

    val siblingMap by remember(graph) {
        mutableStateOf(
            graph.nodes.values.associate { node ->
                val parentWire = graph.wires.find { it.toNodeId == node.id }
                val siblings = if (parentWire != null) {
                    graph.wires
                        .filter { it.fromNodeId == parentWire.fromNodeId && it.fromHandleId == parentWire.fromHandleId }
                        .mapNotNull { graph.nodes[it.toNodeId] }
                } else {
                    emptyList()
                }
                node.id to siblings
            }
        )
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
                                findWireAt(offset, graph.wires, handlePositions)
                            if (wire != null) {
                                editorViewModel.deleteWire(wire)
                            }
                        }
                    )
                }
        ) {
            graph.wires.forEach { wire ->
                val fromPos = handlePositions["${wire.fromNodeId}-${wire.fromHandleId}"]
                val toPos = handlePositions["${wire.toNodeId}-${wire.toHandleId}"]

                if (fromPos != null && toPos != null) {
                    val isWireActive = isSimulating && activeWires.contains(wire)

                    val path = Path().apply {
                        moveTo(fromPos.x, fromPos.y)
                        val controlPoint1 = Offset(fromPos.x + 100, fromPos.y)
                        val controlPoint2 = Offset(toPos.x - 100, toPos.y)
                        cubicTo(
                            controlPoint1.x,
                            controlPoint1.y,
                            controlPoint2.x,
                            controlPoint2.y,
                            toPos.x,
                            toPos.y
                        )
                    }

                    drawPath(
                        path = path,
                        color = if (isWireActive) Color.Yellow else Color.Gray,
                        style = Stroke(
                            width = if (isWireActive) 4f else 2f,
                            pathEffect = if (isWireActive) PathEffect.dashPathEffect(
                                floatArrayOf(20f, 20f),
                                phase
                            ) else null
                        )
                    )
                }
            }

            wireDragInfo?.let { dragInfo ->
                draggedWireEndPosition?.let { endPos ->
                    val startPos =
                        handlePositions["${dragInfo.fromNodeId}-${dragInfo.fromHandleId}"]
                    if (startPos != null) {
                        val path = Path().apply {
                            moveTo(startPos.x, startPos.y)
                            val controlPoint1 = Offset(startPos.x + 100, startPos.y)
                            val controlPoint2 = Offset(endPos.x - 100, endPos.y)
                            cubicTo(
                                controlPoint1.x,
                                controlPoint1.y,
                                controlPoint2.x,
                                controlPoint2.y,
                                endPos.x,
                                endPos.y
                            )
                        }
                        drawPath(
                            path = path,
                            color = Color.White,
                            style = Stroke(width = 2f)
                        )
                    }
                }
            }
        }

        val nodesToRender = graph.nodes.values.sortedBy { node ->
            if (node.id == topNodeId) 1 else 0
        }

        nodesToRender.forEach { node ->
            val highlightInfo = highlightData[node.id]
            val isNodeActive = isSimulating && activeNodes.contains(node.id)
            key(node.id) {
                Box(
                    modifier = Modifier
                        .offset {
                            val offset = node.position.toOffset()
                            IntOffset(offset.x.roundToInt(), offset.y.roundToInt())
                        }
                        .pointerInput(node.id) {
                            detectTapGestures(
                                onPress = { editorViewModel.bringNodeToFront(node.id) }
                            )
                        }
                        .then(
                            if (isNodeActive) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = Color.Yellow,
                                    shape = RoundedCornerShape(8.dp)
                                )
                            } else {
                                Modifier
                            }
                        )
                ) {
                    val content: @Composable () -> Unit = {
                        canvasCoordinates?.let {
                            when (node) {
                                is StateNode -> RenderStateNode(
                                    node,
                                    mainViewModel,
                                    editorViewModel,
                                    it
                                )

                                is ConditionalNode -> RenderConditionalNode(
                                    node = node,
                                    mainViewModel = mainViewModel,
                                    editorViewModel = editorViewModel,
                                    canvasCoordinates = it,
                                    expanded = node.id in expandedNodes,
                                    onExpandedChange = {
                                        expandedNodes = if (it) {
                                            expandedNodes + node.id
                                        } else {
                                            expandedNodes - node.id
                                        }
                                    }
                                )

                                is BehaviouralNode -> RenderBehaviouralNode(
                                    node,
                                    editorViewModel,
                                    it
                                )

                                is StartNode -> StartNodeView(node, editorViewModel, it)
                                is UtilityNode -> RenderUtilityNode(node, editorViewModel, it)
                            }
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

                        val siblings = siblingMap[node.id] ?: emptyList()
                        if (siblings.size > 1) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .offset(x = 12.dp, y = (-12).dp)
                            ) {
                                BranchPriorityDropdown(node, siblings, editorViewModel)
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
}

@Composable
private fun BranchPriorityDropdown(
    node: Node,
    siblings: List<Node>,
    editorViewModel: NodeEditorViewModel
) {
    var expanded by remember { mutableStateOf(false) }
    val sortedSiblings = siblings.sortedBy { it.branchPriority }


    val items = (1..sortedSiblings.size).toList()
    val currentNodeIndex = sortedSiblings.indexOfFirst { it.id == node.id }

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
                            val nodeToSwapWith = sortedSiblings[newIndex]

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
    wires: List<Wire>,
    handlePositions: Map<String, Offset>
): Wire? {
    return wires.find { wire ->
        val fromPos = handlePositions["${wire.fromNodeId}-${wire.fromHandleId}"]
        val toPos = handlePositions["${wire.toNodeId}-${wire.toHandleId}"]

        if (fromPos != null && toPos != null) {
            val dist = distanceToCubicBezier(position, fromPos, Offset(fromPos.x + 100, fromPos.y), Offset(toPos.x - 100, toPos.y), toPos)
            dist < 10f // 10px tolerance
        } else {
            false
        }
    }
}

private fun distanceToCubicBezier(p: Offset, p0: Offset, p1: Offset, p2: Offset, p3: Offset): Float {
    var minDistance = Float.MAX_VALUE
    var previousPoint = p0
    val steps = 100

    for (i in 1..steps) {
        val t = i.toFloat() / steps
        val point = (1 - t).toFloat().pow(3) * p0 +
                3 * (1 - t).toFloat().pow(2) * t * p1 +
                3 * (1 - t).toFloat() * t.pow(2) * p2 +
                t.toFloat().pow(3) * p3

        val distance = distanceToSegment(p, previousPoint, point)
        if (distance < minDistance) {
            minDistance = distance
        }
        previousPoint = point
    }

    return minDistance
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
private fun RenderStateNode(node: StateNode, mainViewModel: MainViewModel, editorViewModel: NodeEditorViewModel, canvasCoordinates: LayoutCoordinates) {
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
                uploadsDir = mainViewModel.uploadsDir,
                canvasCoordinates = canvasCoordinates
            )
        }
    }
}

@Composable
private fun RenderBehaviouralNode(
    node: BehaviouralNode,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates
) {
    val troupe by editorViewModel.mainViewModel.troupe.collectAsState()
    val puppets = troupe?.puppets ?: emptyList()
    val idleImage by editorViewModel.mainViewModel.idleImage.collectAsState()
    val graph by editorViewModel.nodeGraph.collectAsState()
    val startNode = graph.nodes[graph.startNodeId] as? StartNode
    val contextualPuppetId = startNode?.puppetId

    when (node) {
        is GoThroughStateNode -> {
            val puppet = puppets.find { it.name == (node.puppetId ?: contextualPuppetId) }
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
                    uploadsDir = editorViewModel.mainViewModel.uploadsDir,
                    canvasCoordinates = canvasCoordinates
                )
            }
        }

        is WithEffectNode -> {
            val puppet = puppets.find { it.name == (node.puppetId ?: contextualPuppetId) }
            val puppetStates = puppet?.states ?: emptyList()
            val stateInfo = puppetStates.find { it.name == "idle" }
            idleImage?.let { bitmap ->
                WithEffectNodeView(
                    node = node,
                    editorViewModel = editorViewModel,
                    canvasCoordinates = canvasCoordinates,
                    puppetState = stateInfo,
                    idleImage = bitmap,
                    uploadsDir = editorViewModel.mainViewModel.uploadsDir
                )
            }
        }

        is WithLayerNode -> {
            val puppet = puppets.find { it.name == (node.puppetId ?: contextualPuppetId) }
            val puppetStates = puppet?.states ?: emptyList()
            val stateInfo = puppetStates.find { it.name == node.previewStateName }
            val uploadsDir = editorViewModel.mainViewModel.uploadsDir
            val layerImage by produceState<ImageBitmap?>(initialValue = null, node.layer.imageName) {
                if (node.layer.imageName.isNotEmpty()) {
                    val byteArray = readFileAsByteArray(uploadsDir, node.layer.imageName)
                    if (byteArray != null) {
                        value = decodeToImageBitmap(byteArray)
                    }
                }
            }

            val stateImage by produceState<ImageBitmap?>(initialValue = null, stateInfo?.imageName) {
                if (stateInfo?.imageName?.isNotEmpty() == true) {
                    val byteArray = readFileAsByteArray(uploadsDir, stateInfo.imageName)
                    if (byteArray != null) {
                        value = decodeToImageBitmap(byteArray)
                    }
                }
            }
            WithLayerNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates,
                puppetStates = puppetStates,
                layerImage = layerImage,
                stateImage = stateImage,
                uploadsDir = editorViewModel.mainViewModel.uploadsDir,
                selectedStateName = node.previewStateName,
                onStateSelected = { editorViewModel.updateNode(node.copy(previewStateName = it)) }
            )
        }

        is RandomNode -> {
            RandomNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates
            )
        }
    }
}

@Composable
private fun RenderConditionalNode(
    node: ConditionalNode,
    mainViewModel: MainViewModel,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    when (node) {
        is VolumeThresholdNode -> {
            VolumeThresholdNodeView(
                node = node,
                onThresholdChanged = { newThreshold ->
                    editorViewModel.updateNode(node.copy(threshold = newThreshold))
                },
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates,
                expanded = expanded,
                onExpandedChange = onExpandedChange
            )
        }

        is HotKeyNode -> {
            HotKeyNodeView(
                node = node,
                onHotKeyChanged = { newHotKey ->
                    editorViewModel.updateNode(node.copy(hotkey = newHotKey, mode = newHotKey.hold))
                },
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates
            )
        }

        is PhonemeMatchNode -> {
            PhonemeMatchNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates,
                expanded = expanded,
                onExpandedChange = onExpandedChange
            )
        }

        is RhythmNode -> {
            RhythmNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates,
                expanded = expanded,
                onExpandedChange = onExpandedChange
            )
        }
    }
}

@Composable
private fun RenderUtilityNode(node: UtilityNode, editorViewModel: NodeEditorViewModel, canvasCoordinates: LayoutCoordinates) {
    when (node) {
        is SetPuppetNode -> {
            SetPuppetNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates
            )
        }
        is ResetSetNode -> {
            ResetSetNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates
            )
        }
        is DelayTimerNode -> {
            DelayTimerNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates
            )
        }
        is TriggerOnWaitNode -> {
            TriggerOnWaitNodeView(
                node = node,
                editorViewModel = editorViewModel,
                canvasCoordinates = canvasCoordinates
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
        Color(0xFF552C1A), // Brown
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
            val children = graph.wires
                .filter { it.fromNodeId == parentId }
                .mapNotNull { graph.nodes[it.toNodeId] }
                .filter { it.id !in visited }
                .sortedBy { it.branchPriority }

            if (children.isNotEmpty()) {
                val color = colors[colorIndex % colors.size]
                children.forEachIndexed { index, childNode ->
                    highlights[childNode.id] = HighlightInfo(color, index + 1)
                    visited.add(childNode.id)
                    nextLevelParents.add(childNode.id)
                }
                colorIndex++
            }
        }
        parentsToProcess = nextLevelParents
    }

    return highlights
}

operator fun Float.times(offset: Offset): Offset = offset * this
