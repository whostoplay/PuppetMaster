package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.Layer
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize
import org.menagerie.puppet_master.SpecialEffect

@Serializable
data class GoThroughStateNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 310f),
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
    override val size: SerializableSize = SerializableSize(250f, 250f),
    override val expandedSize: SerializableSize? = SerializableSize(275f, 600f),
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

@Serializable
data class WithLayerNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 350f),
    override val expandedSize: SerializableSize? = SerializableSize(800f, 1200f),
    val layer: Layer = Layer(""),
    override val branchPriority: Int = 0,
    val puppetId: String? = null,
    val previewStateName: String = ""
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

@Serializable
data class RandomNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 250f),
    override val expandedSize: SerializableSize? = null,
    val retainOrderDelay: Long = 0,
    override val branchPriority: Int = 0,
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
        WithEffectNode("", SerializableOffset(0f, 0f)),
        WithLayerNode("", SerializableOffset(0f, 0f)),
        RandomNode("", SerializableOffset(0f, 0f))
    )
}
