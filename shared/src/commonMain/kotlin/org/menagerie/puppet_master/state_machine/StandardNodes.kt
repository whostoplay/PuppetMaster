package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize

/**
 * A special node that marks the entry point for the graph execution on each tick.
 * There should be only one StartNode in a graph, and it must be set as the graph's startNodeId.
 * This node produces no action itself; it simply directs the execution flow to the next node.
 */
@Serializable
data class StartNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(180f, 120f),
    override val inputs: List<InputHandle> = emptyList(),
    override val outputs: List<OutputHandle> = listOf(OutputHandle("out"))
) : Node {
    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        // The StartNode's only job is to pass execution to the next node in the graph.
        val nextNodeId = findNextNodeId(graph, "out")
        return ExecuteResult(nextNodeId, null)
    }
}

/**
 * A terminal node that sets the puppet's state. When this node is executed,
 * it returns a SetState action, and the graph execution for the current tick concludes.
 */
@Serializable
data class SetStateNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    val stateName: String, // e.g., "IDLE", "TALK"
    override val size: SerializableSize = SerializableSize(300f, 250f)
) : StateNode {
    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        // SetState is a terminal action for the tick.
        return ExecuteResult(null, GraphAction.SetState(stateName))
    }
}

/**
 * Provides a list of all available nodes for the palette.
 */
fun getAvailableNodes(): List<Node> {
    return listOf(
        StartNode(id = "", position = SerializableOffset(0f, 0f)),
        SetStateNode(id = "", position = SerializableOffset(0f, 0f), stateName = "")
    )
}
