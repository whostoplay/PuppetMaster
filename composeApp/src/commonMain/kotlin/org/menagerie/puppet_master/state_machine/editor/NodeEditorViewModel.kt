package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.ui.geometry.Offset
import cafe.adriel.voyager.core.model.ScreenModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.state_machine.Node
import org.menagerie.puppet_master.state_machine.NodeGraph
import org.menagerie.puppet_master.state_machine.SetStateNode
import org.menagerie.puppet_master.state_machine.Wire
import java.util.UUID

data class WireDragInfo(val fromNodeId: String, val fromHandleId: String)

class NodeEditorViewModel(val mainViewModel: MainViewModel) : ScreenModel {

    private fun createInitialGraph(): NodeGraph {
        val startNode = SetStateNode(
            id = "start",
            position = Offset(50f, 50f),
            stateName = "idle" // Default to idle, can be changed by the user
        )
        return NodeGraph(
            nodes = mapOf(startNode.id to startNode),
            startNodeId = startNode.id
        )
    }

    // Graph State
    private val _nodeGraph = MutableStateFlow(createInitialGraph())
    val nodeGraph: StateFlow<NodeGraph> = _nodeGraph.asStateFlow()

    // Node Drag and Drop State
    private val _draggedNode = MutableStateFlow<Node?>(null)
    val draggedNode: StateFlow<Node?> = _draggedNode.asStateFlow()

    // Wire Drag and Drop State
    private val _wireDragInfo = MutableStateFlow<WireDragInfo?>(null)
    val wireDragInfo: StateFlow<WireDragInfo?> = _wireDragInfo.asStateFlow()

    fun onNodeDragStart(node: Node) {
        _draggedNode.value = node
    }

    fun onNodeDragEnd() {
        _draggedNode.value = null
    }

    fun onWireDragStart(nodeId: String, handleId: String) {
        _wireDragInfo.value = WireDragInfo(nodeId, handleId)
    }

    fun onWireDragEnd() {
        _wireDragInfo.value = null
    }

    fun addNode(templateNode: Node, position: Offset) {
        val newNode = templateNode.copyNode(
            id = UUID.randomUUID().toString(),
            position = position
        )

        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes[newNode.id] = newNode
        _nodeGraph.value = _nodeGraph.value.copy(nodes = newNodes)
    }

    fun addWire(toNodeId: String, toHandleId: String) {
        _wireDragInfo.value?.let { info ->
            val newWire = Wire(
                fromNodeId = info.fromNodeId,
                fromHandleId = info.fromHandleId,
                toNodeId = toNodeId,
                toHandleId = toHandleId
            )
            _nodeGraph.value = _nodeGraph.value.copy(wires = _nodeGraph.value.wires + newWire)
        }
    }

    fun updateNode(node: Node) {
        val newNodes = _nodeGraph.value.nodes.toMutableMap()
        newNodes[node.id] = node
        _nodeGraph.value = _nodeGraph.value.copy(nodes = newNodes)
    }
}
