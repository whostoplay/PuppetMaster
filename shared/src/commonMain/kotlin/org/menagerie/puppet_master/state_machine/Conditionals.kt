package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize

/**
 * A node that branches based on the audio volume.
 */
@Serializable
data class VolumeThresholdNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val threshold: Float = 0.5f,
    override val size: SerializableSize = SerializableSize(200f, 120f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        println("volume is ${context.microphoneVolume} and threshold is $threshold")
        val handleId = if (context.microphoneVolume > threshold) "true" else "false"
        val nextNodeId = findNextNodeId(graph, handleId)
        return ExecuteResult(nextNodeId)
    }
}

/**
 * A node that branches based on a hotkey press.
 */
@Serializable
data class HotKeyNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val hotkey: Hotkey = Hotkey(-1),
    val mode: Boolean = false, // Corresponds to Hotkey.hold
    override val size: SerializableSize = SerializableSize(250f, 200f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val handleId = if (mode) { // Hold mode
            if (context.hotKeyPressed?.shallowEquals(hotkey) == true) "true" else "false"
        } else { // Toggle mode
            if (context.toggledOnNodes.contains(id)) "true" else "false"
        }
        val nextNodeId = findNextNodeId(graph, handleId)
        return ExecuteResult(nextNodeId)
    }
}

/**
 * Provides a list of all available conditional nodes for the palette.
 */
fun getAvailableConditionalNodes(): List<Node> {
    return listOf(
        VolumeThresholdNode(id = "", position = SerializableOffset(0f, 0f)),
        HotKeyNode(id = "", position = SerializableOffset(0f, 0f))
    )
}
