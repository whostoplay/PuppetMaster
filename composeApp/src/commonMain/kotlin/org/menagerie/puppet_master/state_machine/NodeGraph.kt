package org.menagerie.puppet_master.state_machine

/**
 * Represents a wire connecting two nodes in the graph.
 */
data class Wire(
    val fromNodeId: NodeId,
    val fromHandleId: String,
    val toNodeId: NodeId,
    val toHandleId: String
)

/**
 * The complete graph, containing all nodes and the wires connecting them.
 */
data class NodeGraph(
    val nodes: Map<NodeId, Node> = emptyMap(),
    val wires: List<Wire> = emptyList(),
    val startNodeId: String? = null
)
