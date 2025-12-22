package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize

const val ANY_STATE = "ANY STATE"

/**
 * A node that sets the puppet's state.
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
        val nextNodeId = findNextNodeId(graph, "out")
        // "ANY STATE" is a special state that doesn't produce an action itself,
        // but serves as an entry point for global transitions.
        val action = if (stateName == ANY_STATE) null else GraphAction.SetState(stateName)
        return ExecuteResult(nextNodeId, action)
    }
}

/**
 * Provides a list of all available state nodes for the palette.
 */
fun getAvailableStateNodes(): List<Node> {
    return listOf(
        SetStateNode(id = "", position = SerializableOffset(0f, 0f), stateName = "")
    )
}
