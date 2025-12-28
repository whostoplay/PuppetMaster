package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize

enum class SetPuppetMode {
    SWITCH, // One-time change for the current execution path
    SET     // Persists as the new starting point for the graph
}

@Serializable
data class SetPuppetNode(
    override val id: NodeId,
    val puppetId: String? = null,
    val mode: SetPuppetMode = SetPuppetMode.SWITCH,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(250f, 220f),
    override val expandedSize: SerializableSize? = null,
    override val branchPriority: Int = 0
) : UtilityNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node {
        return copy(id = id, position = position)
    }

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val nextNodeId = findNextNodeId(graph, "out")
        val action = if (mode == SetPuppetMode.SET) {
            GraphAction.SetGraphStart(id)
        } else {
            null
        }
        return ExecuteResult(nextNodeId, action)
    }
}

@Serializable
data class ResetSetNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(150f, 100f),
    override val expandedSize: SerializableSize? = null,
    override val branchPriority: Int = 0
) : UtilityNode {
    // This is a terminal node for its branch.
    override val outputs: List<OutputHandle> = emptyList()

    override fun copyNode(id: NodeId, position: SerializableOffset): Node {
        return copy(id = id, position = position)
    }

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        // This node's action stops the graph execution and resets the start point.
        return ExecuteResult(null, GraphAction.ResetGraphStart)
    }
}

@Serializable
data class DelayTimerNode(
    override val id: NodeId,
    val delay: Long = 1000, // Delay in milliseconds
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(180f, 120f),
    override val expandedSize: SerializableSize? = null,
    override val branchPriority: Int = 0
) : UtilityNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node {
        return copy(id = id, position = position)
    }

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val nextNodeId = findNextNodeId(graph, "out")
        return ExecuteResult(nextNodeId)
    }
}

@Serializable
data class TriggerOnWaitNode(
    override val id: NodeId,
    val waitMillis: Long = 1000, // Time to wait in milliseconds before triggering
    override val position: SerializableOffset,
    override val size: SerializableSize = SerializableSize(180f, 120f),
    override val expandedSize: SerializableSize? = null,
    override val branchPriority: Int = 0
) : UtilityNode {
    // This node will only pass if the wait time has been met.
    override val outputs: List<OutputHandle> = listOf(OutputHandle("out"))

    override fun copyNode(id: NodeId, position: SerializableOffset): Node {
        return copy(id = id, position = position)
    }

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        // The actual logic is in the executor. This just specifies what to do on trigger.
        val nextNodeId = findNextNodeId(graph, "out")
        return ExecuteResult(nextNodeId)
    }
}


fun getAvailableUtilityNodes(): List<Node> {
    return listOf(
        SetPuppetNode(id = "", position = SerializableOffset(0f, 0f)),
        ResetSetNode(id = "", position = SerializableOffset(0f, 0f)),
        DelayTimerNode(id = "", position = SerializableOffset(0f, 0f)),
        TriggerOnWaitNode(id = "", position = SerializableOffset(0f, 0f))
    )
}
