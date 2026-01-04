package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.contextual
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize

// A unique identifier for a node
typealias NodeId = String

val nodeSerializersModule = SerializersModule {
    polymorphic(Node::class) {
        subclass(SetStateNode::class)
        subclass(VolumeThresholdNode::class)
        subclass(HotKeyNode::class)
        subclass(StartNode::class)
        subclass(SetPuppetNode::class)
        subclass(ResetSetNode::class)
        subclass(GoThroughStateNode::class)
        subclass(DelayTimerNode::class)
        subclass(TriggerOnWaitNode::class)
        subclass(WithEffectNode::class)
        subclass(PhonemeMatchNode::class)
        subclass(WithLayerNode::class)
        subclass(RandomNode::class)
    }
    contextual(ClosedFloatRangeSerializer)
}

@Serializable
sealed interface Handle {
    val id: String
}

// Represents a connection point on a node
@Serializable
data class InputHandle(override val id: String) : Handle
@Serializable
data class OutputHandle(override val id: String) : Handle

/**
 * The result of a node's execution.
 * @param nextNodeId The ID of the next node to execute, determined by following a wire.
 * @param action An optional action for the UI to perform (e.g., changing state).
 * @param alternativeNextNodeId An alternative node ID for special cases like the TriggerOnWaitNode.
 */
data class ExecuteResult(
    val nextNodeId: NodeId?,
    val action: GraphAction? = null,
    val alternativeNextNodeId: NodeId? = null
)

/**
 * The base for all nodes in the graph. Using a sealed interface ensures
 * all nodes must belong to one of the defined sub-types.
 */
@Serializable
sealed interface Node {
    val id: NodeId
    val position: SerializableOffset // For the UI
    val inputs: List<InputHandle>
    val outputs: List<OutputHandle>
    val size: SerializableSize
    val expandedSize: SerializableSize?
    val branchPriority: Int

    /**
     * Creates a copy of this node with a new ID and position.
     * This allows for a generic way to instantiate nodes from a template.
     */
    fun copyNode(id: NodeId, position: SerializableOffset): Node

    /**
     * Creates a copy of this node with a new branch priority.
     * This is essential for reordering branches in the UI.
     */
    fun copyNodeWithNewPriority(priority: Int): Node


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
@Serializable
sealed interface ConditionalNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("true"))
}

/**
 * BEHAVIOURAL Nodes: Trigger a one-off action in a puppet.
 * These are usually a single step in a flow.
 */
@Serializable
sealed interface BehaviouralNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}

/**
 * STATE Nodes: Represent a puppet being in a specific state or switching to one.
 * By default, these are terminal nodes.
 */
@Serializable
sealed interface StateNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = emptyList()
}

/**
 * EFFECT Nodes: Apply a temporary or permanent visual/audio effect.
 */
@Serializable
sealed interface EffectNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}

/**
 * UTILITY Nodes: For miscellaneous, powerful actions like switching puppets.
 */
@Serializable
sealed interface UtilityNode : Node {
    override val inputs: List<InputHandle> get() = listOf(InputHandle("in"))
    override val outputs: List<OutputHandle> get() = listOf(OutputHandle("out"))
}
