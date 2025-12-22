package org.menagerie.puppet_master.state_machine

import androidx.compose.ui.geometry.Offset

/**
 * A conditional node that checks if the microphone volume is above a certain threshold.
 */
data class VolumeThresholdNode(
    override val id: NodeId,
    override val position: Offset,
    val threshold: Float = 0.5f
) : ConditionalNode {
    override fun copyNode(id: NodeId, position: Offset): Node = this.copy(id = id, position = position)

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val conditionMet = context.microphoneVolume >= threshold
        val handleId = if (conditionMet) "true" else "false"
        val nextNodeId = findNextNodeId(graph, handleId)
        return ExecuteResult(nextNodeId)
    }
}

/**
 * Provides a list of all available conditional nodes for the palette.
 */
fun getAvailableConditionalNodes(): List<Node> {
    return listOf(
        VolumeThresholdNode(id = "", position = Offset.Zero)
    )
}
