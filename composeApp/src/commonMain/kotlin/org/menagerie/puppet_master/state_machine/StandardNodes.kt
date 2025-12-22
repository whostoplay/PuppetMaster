package org.menagerie.puppet_master.state_machine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * A node that sets the puppet's state.
 */
data class SetStateNode(
    override val id: NodeId,
    override val position: Offset,
    val stateName: String, // e.g., "IDLE", "TALK"
    override val size: Size = Size(200f, 220f)
) : StateNode {
    override fun copyNode(id: NodeId, position: Offset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val nextNodeId = findNextNodeId(graph, "out")
        val action = GraphAction.SetState(stateName)
        return ExecuteResult(nextNodeId, action)
    }
}

/**
 * Provides a list of all available state nodes for the palette.
 */
fun getAvailableStateNodes(): List<Node> {
    return listOf(
        SetStateNode(id = "", position = Offset.Zero, stateName = "")
    )
}
