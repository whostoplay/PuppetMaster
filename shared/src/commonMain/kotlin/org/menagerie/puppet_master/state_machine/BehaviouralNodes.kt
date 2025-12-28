package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize
import org.menagerie.puppet_master.SpecialEffect

@Serializable
data class GoThroughStateNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 300f),
    override val expandedSize: SerializableSize? = null,
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

@Serializable
data class WithEffectNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 225f),
    override val expandedSize: SerializableSize? = SerializableSize(250f, 800f),
    val effect: SpecialEffect = SpecialEffect(),
    override val branchPriority: Int = 0,
    val puppetId: String? = null
) : BehaviouralNode {

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val nextNodeId = findNextNodeId(graph, "out")
        return ExecuteResult(nextNodeId = nextNodeId)
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
        GoThroughStateNode("", SerializableOffset(0f, 0f)),
        WithEffectNode("", SerializableOffset(0f, 0f))
    )
}
