package org.menagerie.puppet_master.state_machine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size

/**
 * A node that branches based on the audio volume.
 */
data class VolumeThresholdNode(
    override val id: NodeId,
    override val position: Offset,
    val threshold: Float = 0.5f,
    override val size: Size = Size(200f, 120f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: Offset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val handleId = if (context.microphoneVolume > threshold) "true" else "false"
        val nextNodeId = findNextNodeId(graph, handleId)
        return ExecuteResult(nextNodeId)
    }
}

/**
 * A node that branches based on a hotkey press.
 */
data class HotKeyNode(
    override val id: NodeId,
    override val position: Offset,
    val hotKey: String = "",
    override val size: Size = Size(200f, 120f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: Offset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val handleId = if (context.hotKeyPressed == hotKey) "true" else "false"
        val nextNodeId = findNextNodeId(graph, handleId)
        return ExecuteResult(nextNodeId)
    }
}

/**
 * Provides a list of all available conditional nodes for the palette.
 */
fun getAvailableConditionalNodes(): List<Node> {
    return listOf(
        VolumeThresholdNode(id = "", position = Offset.Zero),
        HotKeyNode(id = "", position = Offset.Zero)
    )
}
