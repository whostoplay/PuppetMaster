package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize

@Serializable
data class GoThroughStateNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 300f),
    val stateName: String = "",
    val delay: Long = 100, // Milliseconds
    override val branchPriority: Int = 0,
    val puppetId: String? = null,
) : BehaviouralNode {

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val nextNodeId = findNextNodeId(graph, "out")
        val finalPuppetId = puppetId ?: context.puppetId
        return ExecuteResult(
            nextNodeId = nextNodeId,
            action = GraphAction.SetState(stateName, finalPuppetId)
        )
    }

    override fun copyNode(id: NodeId, position: SerializableOffset): Node {
        return copy(id = id, position = position)
    }

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return copy(branchPriority = priority)
    }
}

fun getAvailableBehaviouralNodes(): List<Node> {
    return listOf(
        GoThroughStateNode("", SerializableOffset(0f,0f))
    )
}
