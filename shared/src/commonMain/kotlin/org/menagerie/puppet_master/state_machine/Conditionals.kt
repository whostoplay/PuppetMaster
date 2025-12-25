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

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
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

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        if (mode) { // Hold mode
            val handleId = if (context.hotKeyPressed?.shallowEquals(hotkey) == true) "true" else "false"
            val nextNodeId = findNextNodeId(graph, handleId)
            return ExecuteResult(nextNodeId)
        } else { // Toggle mode
            val keyWasPressed = context.hotKeyPressed?.shallowEquals(hotkey) == true &&
                    context.hotKeyPressed?.shallowEquals(context.lastProcessedHotkey) != true

            val isCurrentlyOn = context.toggledOnNodes.contains(id)

            val shouldBeOn = if (keyWasPressed) !isCurrentlyOn else isCurrentlyOn

            val handleId = if (shouldBeOn) "true" else "false"
            val nextNodeId = findNextNodeId(graph, handleId)

            return ExecuteResult(
                nextNodeId = nextNodeId,
                action = if (keyWasPressed) GraphAction.RequestToggle(id) else null
            )
        }
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
