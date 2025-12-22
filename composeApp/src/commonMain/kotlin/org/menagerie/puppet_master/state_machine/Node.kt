package org.menagerie.puppet_master.state_machine

import androidx.compose.ui.geometry.Offset

// A unique identifier for a node
typealias NodeId = String

// Represents a connection point on a node
data class InputHandle(val id: String)
data class OutputHandle(val id: String)

/**
 * The result of a node's execution.
 * @param nextNodeId The ID of the next node to execute, determined by following a wire.
 * @param action An optional action for the UI to perform (e.g., changing state).
 */
data class ExecuteResult(val nextNodeId: NodeId?, val action: GraphAction? = null)

/**
 * The base for all nodes in the graph. Using a sealed interface ensures
 * all nodes must belong to one of the defined sub-types.
 */
sealed interface Node {
    val id: NodeId
    val position: Offset // For the UI
    val inputs: List<InputHandle>
    val outputs: List<OutputHandle>

    /**
     * Creates a copy of this node with a new ID and position.
     * This allows for a generic way to instantiate nodes from a template.
     */
    fun copyNode(id: NodeId, position: Offset): Node

    /**
     * Executes the node's specific logic.
     * @param context The live data needed for decision making (e.g., volume).
     * @param graph The overall graph structure, used to find connected nodes.
     * @return An [ExecuteResult] telling the executor what to do next.
     */
    fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult

    fun findNextNodeId(graph: NodeGraph, handleId: String): NodeId? {
        val wire = graph.wires.find { it.fromNodeId == id && it.fromHandleId == handleId }
        return wire?.toNodeId
    }
}

/**
 * CONDITIONAL Nodes: Check against a condition and branch the flow.
 * They typically have one input and multiple output paths (e.g., true/false).
 */
sealed interface ConditionalNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("true"), OutputHandle("false"))
}

/**
 * BEHAVIOURAL Nodes: Trigger a one-off action in a puppet.
 * These are usually a single step in a flow.
 */
sealed interface BehaviouralNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}

/**
 * STATE Nodes: Represent a puppet being in a specific state or switching to one.
 */
sealed interface StateNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}

/**
 * EFFECT Nodes: Apply a temporary or permanent visual/audio effect.
 */
sealed interface EffectNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}

/**
 * UTILITY Nodes: For miscellaneous, powerful actions like switching puppets.
 */
sealed interface UtilityNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}
