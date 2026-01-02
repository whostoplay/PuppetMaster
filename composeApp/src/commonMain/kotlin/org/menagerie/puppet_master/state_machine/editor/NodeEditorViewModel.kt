package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.IntSize
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.state_machine.GoThroughStateNode
import org.menagerie.puppet_master.state_machine.GraphExecutionContext
import org.menagerie.puppet_master.state_machine.GraphExecutor
import org.menagerie.puppet_master.state_machine.Handle
import org.menagerie.puppet_master.state_machine.Node
import org.menagerie.puppet_master.state_machine.NodeGraph
import org.menagerie.puppet_master.state_machine.SetPuppetNode
import org.menagerie.puppet_master.state_machine.SetStateNode
import org.menagerie.puppet_master.state_machine.StartNode
import org.menagerie.puppet_master.state_machine.Wire
import org.menagerie.puppet_master.toOffset
import org.menagerie.puppet_master.toSerializableOffset
import org.menagerie.puppet_master.toSize
import java.util.UUID

data class WireDragInfo(val fromNodeId: String, val fromHandleId: String)
data class NodeDragInfo(val nodeId: String)

enum class Arrangement {
    SHUFFLE, UP, DOWN
}

class NodeEditorViewModel(val mainViewModel: MainViewModel) : ScreenModel {

    private val _lastInteractedNodeId = MutableStateFlow<String?>(null)
    val lastInteractedNodeId: StateFlow<String?> = _lastInteractedNodeId.asStateFlow()

    // Graph State
    private val _nodeGraph =
        MutableStateFlow(mainViewModel.troupe.value?.nodeGraph ?: NodeGraph.createInitialGraph())
    val nodeGraph: StateFlow<NodeGraph> = _nodeGraph.asStateFlow()

    // Highlight Mode
    private val _highlightMode = MutableStateFlow(false)
    val highlightMode: StateFlow<Boolean> = _highlightMode.asStateFlow()

    // Simulation Mode
    private val _isSimulating = MutableStateFlow(false)
    val isSimulating: StateFlow<Boolean> = _isSimulating.asStateFlow()
    private val _activeNodes = MutableStateFlow<Set<String>>(emptySet())
    val activeNodes: StateFlow<Set<String>> = _activeNodes.asStateFlow()
    private val _activeWires = MutableStateFlow<Set<Wire>>(emptySet())
    val activeWires: StateFlow<Set<Wire>> = _activeWires.asStateFlow()
    private var simulationJob: Job? = null
    private var graphExecutor: GraphExecutor? = null
    private val _lastPressedKey = MutableStateFlow<Hotkey?>(null)

    // Arrangement
    private val _arrangement = MutableStateFlow(Arrangement.SHUFFLE)
    val arrangement: StateFlow<Arrangement> = _arrangement.asStateFlow()

    // Menus
    private val _contextMenuPosition = MutableStateFlow<Offset?>(null)
    val contextMenuPosition: StateFlow<Offset?> = _contextMenuPosition.asStateFlow()
    private var wireDragInfoForMenu: WireDragInfo? = null

    fun cycleArrangement() {
        val nextArrangement = when (arrangement.value) {
            Arrangement.SHUFFLE -> Arrangement.UP
            Arrangement.UP -> Arrangement.DOWN
            Arrangement.DOWN -> Arrangement.SHUFFLE
        }

        setArrangement(nextArrangement)
    }

        private fun setArrangement(newArrangement: Arrangement) {
            _arrangement.value = newArrangement
            if (newArrangement == Arrangement.SHUFFLE) return

            val currentGraph = _nodeGraph.value
            val newNodes = currentGraph.nodes.toMutableMap()

            val nodesByParent = currentGraph.wires.groupBy { it.fromNodeId }

            for ((_, wires) in nodesByParent) {
                val siblings = wires.mapNotNull { currentGraph.nodes[it.toNodeId] }
                if (siblings.size <= 1) continue

                val sortedSiblings = when (newArrangement) {
                    Arrangement.UP -> siblings.sortedBy { it.position.y }
                    Arrangement.DOWN -> siblings.sortedByDescending { it.position.y }
                    Arrangement.SHUFFLE -> return
                }

                sortedSiblings.forEachIndexed { index, node ->
                    if (node.branchPriority != index) {
                        newNodes[node.id] = node.copyNodeWithNewPriority(index)
                    }
                }
            }
            commitGraphUpdate(currentGraph.copy(nodes = newNodes))
        }

    fun sortNodes() {
        val currentGraph = _nodeGraph.value
        val startNode = currentGraph.startNodeId?.let { currentGraph.nodes[it] } ?: return

        val newNodes = currentGraph.nodes.toMutableMap()
        val nodeDepths = mutableMapOf<String, Int>()
        val nodeChildren = currentGraph.wires.groupBy { it.fromNodeId }
            .mapValues { entry -> entry.value.map { it.toNodeId }.distinct() }

        // 1. Calculate MAX depths for all nodes to handle complex DAGs
        val queue = ArrayDeque<Pair<String, Int>>()
        // Initialize all depths to -1 to indicate they haven't been optimally set
        currentGraph.nodes.keys.forEach { nodeDepths[it] = -1 }

        queue.add(startNode.id to 0)
        nodeDepths[startNode.id] = 0

        while (queue.isNotEmpty()) {
            val (currentNodeId, currentDepth) = queue.removeFirst()

            nodeChildren[currentNodeId]?.forEach { childId ->
                // A child's depth should be the max of its current calculated depth
                // and the new path's depth. This ensures it's placed at the furthest column it belongs to.
                if (currentDepth + 1 > nodeDepths[childId]!!) {
                    nodeDepths[childId] = currentDepth + 1
                    queue.add(childId to currentDepth + 1)
                }
            }
        }

        // 2. Calculate column X positions
        val columnWidths = mutableMapOf<Int, Float>()
        for ((nodeId, depth) in nodeDepths) {
            if (depth == -1) continue // Skip nodes unreachable from start
            val node = newNodes[nodeId] ?: continue
            val currentWidth = columnWidths.getOrDefault(depth, 0f)
            columnWidths[depth] = maxOf(currentWidth, node.size.width.toFloat())
        }

        val columnX = mutableMapOf<Int, Float>()
        var accumulatedX = startNode.position.toOffset().x
        for (depth in 0..(nodeDepths.values.maxOrNull() ?: 0)) {
            columnX[depth] = accumulatedX
            accumulatedX += (columnWidths.getOrDefault(depth, 0f) + 80f) // Add spacing
        }

        // 3. Position nodes recursively
        val columnYTrackers = mutableMapOf<Int, Float>()
        val processedNodes = mutableSetOf<String>()
        val branchHeightCache = mutableMapOf<String, Float>()

        fun getNodeBranchHeight(nodeId: String): Float {
            if (branchHeightCache.containsKey(nodeId)) return branchHeightCache[nodeId]!!
            val node = newNodes[nodeId] ?: return 0f
            val children = nodeChildren[nodeId]?.mapNotNull { newNodes[it] } ?: emptyList()

            if (children.isEmpty()) {
                branchHeightCache[nodeId] = node.size.height.toFloat()
                return node.size.height.toFloat()
            }
            // Sum of children's branch heights plus spacing
            val height = children.sumOf { getNodeBranchHeight(it.id).toDouble() }.toFloat() + (children.size - 1) * 20f
            branchHeightCache[nodeId] = height
            return height
        }

        fun positionNodesRecursively(nodeId: String, yPos: Float) {
            // Because nodes can have multiple parents, we must prevent reprocessing.
            if (nodeId in processedNodes) return
            processedNodes.add(nodeId)

            val node = newNodes[nodeId] ?: return
            val depth = nodeDepths[nodeId] ?: return
            if (depth == -1) return // Skip unreachable node

            val nodeX = columnX[depth] ?: 0f
            val nodeHeight = node.size.height.toFloat()

            // Update node's own position
            newNodes[nodeId] = node.copyNode(node.id, Offset(nodeX, yPos).toSerializableOffset())

            val children = (nodeChildren[nodeId]?.mapNotNull { newNodes[it] } ?: emptyList())
                .sortedBy { it.branchPriority }
            val arrangedChildren = if (arrangement.value == Arrangement.DOWN) children.reversed() else children

            if (arrangedChildren.isEmpty()) return

            val totalBranchHeight = arrangedChildren.sumOf { getNodeBranchHeight(it.id).toDouble() }.toFloat() + (arrangedChildren.size - 1) * 20f
            var nextY = yPos + (nodeHeight / 2f) - (totalBranchHeight / 2f)

            for (child in arrangedChildren) {
                val childDepth = nodeDepths[child.id] ?: continue
                val childBranchHeight = getNodeBranchHeight(child.id)
                val childCenterY = nextY + (childBranchHeight / 2f)
                val childTopY = childCenterY - (child.size.height.toFloat() / 2f)

                // Use the greater of the calculated Y or the last known Y for that column to prevent overlaps
                val finalY = maxOf(childTopY, columnYTrackers.getOrDefault(childDepth, Float.NEGATIVE_INFINITY))

                // Since a node can be a child of multiple parents, we only recurse if it hasn't been placed yet.
                // Its position will be determined by its first parent in the traversal.
                positionNodesRecursively(child.id, finalY)

                val newOccupiedY = finalY + childBranchHeight + 20f
                columnYTrackers[childDepth] = newOccupiedY
                nextY += childBranchHeight + 20f
            }
        }

        positionNodesRecursively(startNode.id, startNode.position.toOffset().y)
        commitGraphUpdate(currentGraph.copy(nodes = newNodes))
    }

    fun toggleHighlightMode() {
        _highlightMode.value = !_highlightMode.value
    }

    // Wire Drag and Drop State
    private val _wireDragInfo = MutableStateFlow<WireDragInfo?>(null)
    val wireDragInfo: StateFlow<WireDragInfo?> = _wireDragInfo.asStateFlow()
    private val _draggedWireEndPosition = MutableStateFlow<Offset?>(null)
    val draggedWireEndPosition: StateFlow<Offset?> = _draggedWireEndPosition.asStateFlow()


    // Node Drag and Drop State
    private val _draggedNodeInfo = MutableStateFlow<NodeDragInfo?>(null)

    // Handle Positions
    private val _handlePositions = MutableStateFlow<Map<String, Offset>>(emptyMap())
    val handlePositions: StateFlow<Map<String, Offset>> = _handlePositions.asStateFlow()

    private val _canvasSize = MutableStateFlow(IntSize.Zero)

    init {
        val graph = _nodeGraph.value
        val startNode = graph.startNodeId?.let { graph.nodes[it] }

        if (graph.startNodeId != null && (startNode == null || startNode !is StartNode)) {
            val correctedGraph = graph.copy(startNodeId = null)
            commitGraphUpdate(correctedGraph)
        }
    }

    private fun commitGraphUpdate(newGraph: NodeGraph) {
        _nodeGraph.value = newGraph
        mainViewModel.updateNodeGraph(newGraph)
        if (_isSimulating.value) {
            stopSimulation()
            startSimulation()
        }
    }

    fun toggleSimulation() {
        if (isSimulating.value) {
            stopSimulation()
        } else {
            startSimulation()
        }
    }

    fun onKeyEvent(keyEvent: KeyEvent) {
        val hotkey = Hotkey(
            keyEvent.key.keyCode,
            keyEvent.isShiftPressed,
            keyEvent.isCtrlPressed,
            keyEvent.isAltPressed,
        )
        if (keyEvent.type == KeyEventType.KeyDown) {
            _lastPressedKey.value = hotkey
        } else if (keyEvent.type == KeyEventType.KeyUp) {
            if (_lastPressedKey.value?.shallowEquals(hotkey) == true) {
                _lastPressedKey.value = null
            }
        }
    }

    private fun startSimulation() {
        _isSimulating.value = true
        graphExecutor = GraphExecutor(_nodeGraph.value)
        simulationJob = screenModelScope.launch {
            while (_isSimulating.value) {
                val context = GraphExecutionContext(
                    microphoneVolume = mainViewModel.audioLevel.value,
                    hotKeyPressed = _lastPressedKey.value,
                    frequencyPeaks = mainViewModel.frequencyPeaks.value
                )
                graphExecutor?.tick(context)
                _activeNodes.value = graphExecutor?.getActiveNodes() ?: emptySet()
                _activeWires.value = graphExecutor?.getActiveWires() ?: emptySet()
                delay(100)
            }
        }
    }

    private fun stopSimulation() {
        _isSimulating.value = false
        simulationJob?.cancel()
        _activeNodes.value = emptySet()
        _activeWires.value = emptySet()
        graphExecutor?.reset()
    }

    fun bringNodeToFront(nodeId: String) {
        _lastInteractedNodeId.value = nodeId
    }

    fun onWireDragStart(nodeId: String, handleId: String) {
        _wireDragInfo.value = WireDragInfo(nodeId, handleId)
        val startPosition = _handlePositions.value["$nodeId-$handleId"]
        _draggedWireEndPosition.value = startPosition
        _lastInteractedNodeId.value = nodeId
    }

    fun onWireDrag(dragAmount: Offset) {
        _draggedWireEndPosition.value = _draggedWireEndPosition.value?.plus(dragAmount)
    }

    fun onWireDragEnd() {
        val currentDragInfo = _wireDragInfo.value
        val endPosition = _draggedWireEndPosition.value
        _wireDragInfo.value = null
        _draggedWireEndPosition.value = null

        if (endPosition == null || currentDragInfo == null) return

        val targetHandle = findHandleAt(endPosition)
        if (targetHandle != null) {
            val (targetNode, handle) = targetHandle
            if (targetNode.id != currentDragInfo.fromNodeId) {
                addWire(currentDragInfo.fromNodeId, currentDragInfo.fromHandleId, targetNode.id, handle.id)
            }
        } else {
            wireDragInfoForMenu = currentDragInfo
            _contextMenuPosition.value = endPosition
        }
    }

    private fun findHandleAt(position: Offset): Pair<Node, Handle>? {
        for (node in _nodeGraph.value.nodes.values) {
            val allHandles: List<Handle> = node.inputs + node.outputs
            for (handle in allHandles) {
                val handlePosition = _handlePositions.value["${node.id}-${handle.id}"]
                if (handlePosition != null) {
                    val distance = (position - handlePosition).getDistance()
                    if (distance < 20f) { // 20px tolerance
                        return Pair(node, handle)
                    }
                }
            }
        }
        return null
    }

    fun onNodeDragStart(nodeId: String) {
        _draggedNodeInfo.value = NodeDragInfo(nodeId)
        _lastInteractedNodeId.value = nodeId
    }

    fun onNodeDrag(dragAmount: SerializableOffset) {
        _draggedNodeInfo.value?.let { dragInfo ->
            val nodes = _nodeGraph.value.nodes
            val draggedNode = nodes[dragInfo.nodeId]
            if (draggedNode != null) {
                val newPosition = draggedNode.position.toOffset() + dragAmount.toOffset()
                val newNodes = nodes.toMutableMap()
                newNodes[dragInfo.nodeId] = draggedNode.copyNode(draggedNode.id, newPosition.toSerializableOffset())
                _nodeGraph.value = _nodeGraph.value.copy(nodes = newNodes)
            }
        }
    }

    fun onNodeDragEnd() {
        _draggedNodeInfo.value = null
        setArrangement(arrangement.value)
        mainViewModel.updateNodeGraph(nodeGraph.value)
        if (_isSimulating.value) {
            stopSimulation()
            startSimulation()
        }
    }

    fun addNode(templateNode: Node) {
        if (templateNode is StartNode && _nodeGraph.value.startNodeId != null) {
            return
        }

        val contextMenuPos = _contextMenuPosition.value
        val wasWireDragged = wireDragInfoForMenu != null
        val finalPosition: Offset

        if (contextMenuPos != null) {
            finalPosition = contextMenuPos
            closeContextMenu()
        } else {
            val spawnOffset = templateNode.size.toSize().width + 50f
            val basePosition = _nodeGraph.value.nodes[lastInteractedNodeId.value ?: _nodeGraph.value.startNodeId]?.position?.toOffset()
                ?: Offset(50f, 15000f) // Fallback to middle-left
            var targetPosition = basePosition.copy(x = basePosition.x + spawnOffset)
            var verticalOffset = 0f
            var offsetMultiplier = 1

            while (isOccupied(Rect(targetPosition, templateNode.size.toSize()))) {
                verticalOffset = (templateNode.size.toSize().height + 20f) * offsetMultiplier
                targetPosition = targetPosition.copy(y = basePosition.y + verticalOffset)
                if (isOccupied(Rect(targetPosition, templateNode.size.toSize()))) {
                    targetPosition = targetPosition.copy(y = basePosition.y - verticalOffset)
                }
                offsetMultiplier++
            }

            val canvasWidth = _canvasSize.value.width
            val nodeWidth = templateNode.size.toSize().width
            if (canvasWidth > 0 && targetPosition.x + nodeWidth > canvasWidth) {
                targetPosition = targetPosition.copy(x = canvasWidth - nodeWidth - 20f)
            }
            finalPosition = targetPosition
        }


        val newNode = templateNode.copyNode(
            id = UUID.randomUUID().toString(),
            position = finalPosition.toSerializableOffset()
        )

        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes[newNode.id] = newNode
        var newGraph = if (newNode is StartNode) {
            _nodeGraph.value.copy(nodes = newNodes, startNodeId = newNode.id)
        } else {
            _nodeGraph.value.copy(nodes = newNodes)
        }

        if (wasWireDragged) {
            wireDragInfoForMenu?.let { dragInfo ->
                val fromNode = newGraph.nodes[dragInfo.fromNodeId]
                val toNode = newNode

                if (fromNode != null) {
                    val fromHandle = fromNode.outputs.find { it.id == dragInfo.fromHandleId }
                    val toHandle = toNode.inputs.firstOrNull()

                    if (fromHandle != null && toHandle != null) {
                        val newWire = Wire(dragInfo.fromNodeId, fromHandle.id, toNode.id, toHandle.id)
                        val newWires = newGraph.wires + newWire
                        newGraph = newGraph.copy(wires = newWires)

                        val puppetId = when (fromNode) {
                            is StartNode -> fromNode.puppetId
                            is SetPuppetNode -> fromNode.puppetId
                            else -> getPuppetIdFromGraph(fromNode.id)
                        }

                        if (puppetId != null) {
                            propagatePuppetId(toNode.id, puppetId, newNodes)
                            newGraph = newGraph.copy(nodes = newNodes) // Capture updates from propagation
                        }
                    }
                }
                wireDragInfoForMenu = null
            }
        }

        _lastInteractedNodeId.value = newNode.id
        commitGraphUpdate(newGraph)
    }

    private fun isOccupied(newRect: Rect): Boolean {
        return _nodeGraph.value.nodes.values.any { node ->
            val otherRect = Rect(node.position.toOffset(), node.size.toSize())
            newRect.overlaps(otherRect)
        }
    }

    private fun addWire(node1Id: String, handle1Id: String, node2Id: String, handle2Id: String) {
        val currentGraph = _nodeGraph.value
        val node1 = currentGraph.nodes[node1Id]
        val node2 = currentGraph.nodes[node2Id]
        if (node1 == null || node2 == null) return

        val handle1IsOutput = node1.outputs.any { it.id == handle1Id }
        val handle2IsInput = node2.inputs.any { it.id == handle2Id }

        val fromNodeId: String
        val fromHandleId: String
        val toNodeId: String
        val toHandleId: String

        if (handle1IsOutput && handle2IsInput) {
            fromNodeId = node1Id
            fromHandleId = handle1Id
            toNodeId = node2Id
            toHandleId = handle2Id
        } else {
            val handle1IsInput = node1.inputs.any { it.id == handle1Id }
            val handle2IsOutput = node2.outputs.any { it.id == handle2Id }
            if (handle1IsInput && handle2IsOutput) {
                fromNodeId = node2Id
                fromHandleId = handle2Id
                toNodeId = node1Id
                toHandleId = handle1Id
            } else {
                return
            }
        }

        val newNodes = currentGraph.nodes.toMutableMap()

        val toNode = newNodes[toNodeId]
        if (toNode != null) {
            val siblings = currentGraph.wires
                .filter { it.fromNodeId == fromNodeId && it.fromHandleId == fromHandleId }
                .mapNotNull { currentGraph.nodes[it.toNodeId] }

            val maxPriority = siblings.maxOfOrNull { it.branchPriority } ?: -1
            newNodes[toNodeId] = toNode.copyNodeWithNewPriority(maxPriority + 1)
        }

        val newWire = Wire(fromNodeId, fromHandleId, toNodeId, toHandleId)
        val newWires = if (!currentGraph.wires.contains(newWire)) {
            currentGraph.wires + newWire
        } else {
            currentGraph.wires
        }

        val fromNode = newNodes[fromNodeId]
        val puppetId = when (fromNode) {
            is StartNode -> fromNode.puppetId
            is SetPuppetNode -> fromNode.puppetId
            else -> getPuppetIdFromGraph(fromNodeId)
        }

        if (puppetId != null) {
            propagatePuppetId(toNodeId, puppetId, newNodes)
        }

        _lastInteractedNodeId.value = toNodeId
        commitGraphUpdate(currentGraph.copy(nodes = newNodes, wires = newWires))
    }

    private fun getPuppetIdFromGraph(nodeId: String): String? {
        val allWires = _nodeGraph.value.wires
        val incomingWire = allWires.find { it.toNodeId == nodeId }
        if (incomingWire != null) {
            return when (val fromNode = _nodeGraph.value.nodes[incomingWire.fromNodeId]) {
                is StartNode -> fromNode.puppetId
                is SetPuppetNode -> fromNode.puppetId
                null -> null
                else -> getPuppetIdFromGraph(fromNode.id)
            }
        }
        return null
    }

    private fun propagatePuppetId(nodeId: String, puppetId: String, nodes: MutableMap<String, Node>) {
        val node = nodes[nodeId]
        if (node is SetPuppetNode) return

        if (node is SetStateNode) {
            nodes[nodeId] = node.copy(puppetId = puppetId)
        }
        if (node is GoThroughStateNode) {
            nodes[nodeId] = node.copy(puppetId = puppetId)
        }

        val wires = _nodeGraph.value.wires.filter { it.fromNodeId == nodeId }
        for (wire in wires) {
            propagatePuppetId(wire.toNodeId, puppetId, nodes)
        }
    }

    private fun rePrioritizeSiblings(fromNodeId: String, fromHandleId: String, graph: NodeGraph): NodeGraph {
        val siblings = graph.wires
            .filter { it.fromNodeId == fromNodeId && it.fromHandleId == fromHandleId }
            .mapNotNull { graph.nodes[it.toNodeId] }
            .sortedBy { it.branchPriority }

        if (siblings.isEmpty()) return graph

        val newNodes = graph.nodes.toMutableMap()
        var changed = false
        siblings.forEachIndexed { index, node ->
            if (node.branchPriority != index) {
                newNodes[node.id] = node.copyNodeWithNewPriority(index)
                changed = true
            }
        }

        return if (changed) graph.copy(nodes = newNodes) else graph
    }

    fun deleteWire(wire: Wire) {
        val currentGraph = _nodeGraph.value
        val updatedWires = currentGraph.wires - wire
        val graphWithUpdatedWires = currentGraph.copy(wires = updatedWires)
        val reprioritizedGraph = rePrioritizeSiblings(wire.fromNodeId, wire.fromHandleId, graphWithUpdatedWires)
        commitGraphUpdate(reprioritizedGraph)
    }

    fun deleteNode(nodeId: String) {
        val currentGraph = _nodeGraph.value
        val node = currentGraph.nodes[nodeId] ?: return

        val newNodes = currentGraph.nodes.toMutableMap()
        newNodes.remove(nodeId)

        val wiresForNode = currentGraph.wires.filter { it.fromNodeId == nodeId || it.toNodeId == nodeId }
        val newWires = currentGraph.wires - wiresForNode.toSet()

        var newGraph = if (nodeId == currentGraph.startNodeId) {
            currentGraph.copy(nodes = newNodes, wires = newWires, startNodeId = null)
        } else {
            currentGraph.copy(nodes = newNodes, wires = newWires)
        }

        val parentWires = wiresForNode.filter { it.toNodeId == nodeId }
        parentWires.forEach { parentWire ->
            newGraph = rePrioritizeSiblings(parentWire.fromNodeId, parentWire.fromHandleId, newGraph)
        }

        commitGraphUpdate(newGraph)
    }

    fun updateCanvasSize(newSize: IntSize) {
        _canvasSize.value = newSize
    }

    fun updateNode(node: Node) {
        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes[node.id] = node

        val puppetId = when (node) {
            is StartNode -> node.puppetId
            is SetPuppetNode -> node.puppetId
            else -> null
        }

        if (puppetId != null) {
            propagatePuppetId(node.id, puppetId, newNodes)
        }

        _lastInteractedNodeId.value = node.id
        commitGraphUpdate(_nodeGraph.value.copy(nodes = newNodes))
    }

    fun swapNodePriorities(nodeIdToMove: String, targetNodeId: String) {
        val currentGraph = _nodeGraph.value
        val nodeToMove = currentGraph.nodes[nodeIdToMove] ?: return

        // Find siblings of the node to move.
        val parentWire = currentGraph.wires.find { it.toNodeId == nodeIdToMove } ?: return
        val siblings = currentGraph.wires
            .filter { it.fromNodeId == parentWire.fromNodeId && it.fromHandleId == parentWire.fromHandleId }
            .mapNotNull { currentGraph.nodes[it.toNodeId] }
            .sortedBy { it.branchPriority }
            .map { it.id }
            .toMutableList()

        val fromIndex = siblings.indexOf(nodeIdToMove)
        val toIndex = siblings.indexOf(targetNodeId)

        if (fromIndex == -1 || toIndex == -1) return

        val item = siblings.removeAt(fromIndex)
        siblings.add(toIndex, item)

        val newNodes = currentGraph.nodes.toMutableMap()
        siblings.forEachIndexed { index, nodeId ->
            val node = newNodes[nodeId]
            if (node != null && node.branchPriority != index) {
                newNodes[nodeId] = node.copyNodeWithNewPriority(index)
            }
        }
        commitGraphUpdate(currentGraph.copy(nodes = newNodes))
    }


    fun updateHandlePosition(nodeId: String, handleId: String, position: Offset) {
        val key = "$nodeId-$handleId"
        val newPositions = _handlePositions.value.toMutableMap()
        newPositions[key] = position
        _handlePositions.value = newPositions
    }

    fun closeContextMenu() {
        _contextMenuPosition.value = null
    }

}
