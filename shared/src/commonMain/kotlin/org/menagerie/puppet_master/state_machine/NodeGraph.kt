package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.SerializableOffset

/**
 * Represents a wire connecting two nodes in the graph.
 */
@Serializable
data class Wire(
    val fromNodeId: NodeId,
    val fromHandleId: String,
    val toNodeId: NodeId,
    val toHandleId: String
)

/**
 * The complete graph, containing all nodes and the wires connecting them.
 */
@Serializable
data class NodeGraph(
    val nodes: Map<NodeId, Node> = emptyMap(),
    val wires: List<Wire> = emptyList(),
    val startNodeId: String? = null
) {
    companion object {
        fun createInitialGraph(): NodeGraph {
            val startNode = SetStateNode(
                id = "start",
                position = SerializableOffset(50f, 50f),
                stateName = "idle"
            )
            return NodeGraph(
                nodes = mapOf(startNode.id to startNode),
                startNodeId = startNode.id
            )
        }
    }
}
