package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.state_machine.Handle
import org.menagerie.puppet_master.state_machine.Node
import org.menagerie.puppet_master.state_machine.NodeGraph
import org.menagerie.puppet_master.state_machine.StartNode
import org.menagerie.puppet_master.state_machine.Wire
import org.menagerie.puppet_master.toOffset
import org.menagerie.puppet_master.toSerializableOffset
import org.menagerie.puppet_master.toSize
import java.util.UUID

data class WireDragInfo(val fromNodeId: String, val fromHandleId: String)
data class NodeDragInfo(val nodeId: String)

class NodeEditorViewModel(val mainViewModel: MainViewModel) : ScreenModel {

    private var lastInteractedNodeId: String? = null

    // Graph State
    private val _nodeGraph = MutableStateFlow(mainViewModel.troupe.value?.nodeGraph ?: NodeGraph.createInitialGraph())
    val nodeGraph: StateFlow<NodeGraph> = _nodeGraph.asStateFlow()

    // Highlight Mode
    private val _highlightMode = MutableStateFlow(false)
    val highlightMode: StateFlow<Boolean> = _highlightMode.asStateFlow()

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

    init {
        val graph = _nodeGraph.value
        val startNode = graph.startNodeId?.let { graph.nodes[it] }

        if (graph.startNodeId != null && (startNode == null || startNode !is StartNode)) {
            // The startNodeId is invalid (points to nothing or not a StartNode).
            // This can happen when loading old graphs. Let's clean it up.
            val correctedGraph = graph.copy(startNodeId = null)
            _nodeGraph.value = correctedGraph
            mainViewModel.updateNodeGraph(correctedGraph)
        }
    }

    fun onWireDragStart(nodeId: String, handleId: String) {
        _wireDragInfo.value = WireDragInfo(nodeId, handleId)
        val startPosition = _handlePositions.value["$nodeId-$handleId"]
        _draggedWireEndPosition.value = startPosition
        lastInteractedNodeId = nodeId
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
        }
        mainViewModel.updateNodeGraph(nodeGraph.value)
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
        lastInteractedNodeId = nodeId
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
        mainViewModel.updateNodeGraph(nodeGraph.value)
    }

    fun addNode(templateNode: Node) {
        if (templateNode is StartNode && _nodeGraph.value.startNodeId != null) {
            // Prevent adding more than one start node
            return
        }

        val spawnOffset = templateNode.size.toSize().width + 50f
        val basePosition = _nodeGraph.value.nodes[lastInteractedNodeId]?.position?.toOffset() ?: Offset(50f, 50f)
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

        val newNode = templateNode.copyNode(
            id = UUID.randomUUID().toString(),
            position = targetPosition.toSerializableOffset()
        )

        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes[newNode.id] = newNode
        val newGraph = if (newNode is StartNode) {
            _nodeGraph.value.copy(nodes = newNodes, startNodeId = newNode.id)
        } else {
            _nodeGraph.value.copy(nodes = newNodes)
        }

        _nodeGraph.value = newGraph
        lastInteractedNodeId = newNode.id
    }

    private fun isOccupied(newRect: Rect): Boolean {
        return _nodeGraph.value.nodes.values.any { node ->
            val otherRect = Rect(node.position.toOffset(), node.size.toSize())
            newRect.overlaps(otherRect)
        }
    }

    private fun addWire(node1Id: String, handle1Id: String, node2Id: String, handle2Id: String) {
        val node1 = _nodeGraph.value.nodes[node1Id]
        val node2 = _nodeGraph.value.nodes[node2Id]
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
                return // Invalid connection (e.g. input-to-input)
            }
        }

        val newWire = Wire(fromNodeId, fromHandleId, toNodeId, toHandleId)
        if (!_nodeGraph.value.wires.contains(newWire)) {
            _nodeGraph.value = _nodeGraph.value.copy(wires = _nodeGraph.value.wires + newWire)
        }
        lastInteractedNodeId = toNodeId
    }

    fun deleteWire(wire: Wire) {
        _nodeGraph.value = _nodeGraph.value.copy(wires = _nodeGraph.value.wires - wire)
        mainViewModel.updateNodeGraph(nodeGraph.value)
    }

    fun deleteNode(nodeId: String) {
        val node = _nodeGraph.value.nodes[nodeId] ?: return

        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes.remove(nodeId)

        val newWires = _nodeGraph.value.wires.filterNot { it.fromNodeId == nodeId || it.toNodeId == nodeId }

        val newGraph = if (nodeId == _nodeGraph.value.startNodeId) {
            _nodeGraph.value.copy(nodes = newNodes, wires = newWires, startNodeId = null)
        } else {
            _nodeGraph.value.copy(nodes = newNodes, wires = newWires)
        }
        _nodeGraph.value = newGraph
        mainViewModel.updateNodeGraph(nodeGraph.value)
    }

    fun updateNode(node: Node) {
        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes[node.id] = node
        _nodeGraph.value = _nodeGraph.value.copy(nodes = newNodes)
        lastInteractedNodeId = node.id
        mainViewModel.updateNodeGraph(nodeGraph.value)
    }

    fun updateHandlePosition(nodeId: String, handleId: String, position: Offset) {
        val key = "$nodeId-$handleId"
        val newPositions = _handlePositions.value.toMutableMap()
        newPositions[key] = position
        _handlePositions.value = newPositions
    }
}
