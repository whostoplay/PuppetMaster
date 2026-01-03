package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize
import org.menagerie.puppet_master.state_machine.ConditionType

/**
 * A node that branches based on the audio volume.
 */
@Serializable
data class VolumeThresholdNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val threshold: Float = 0.5f,
    val sensitivity: Float = 1.0f,
    override val size: SerializableSize = SerializableSize(200f, 180f),
    override val expandedSize: SerializableSize? = null
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val nextNodeId = if (context.microphoneVolume > threshold) {
            findNextNodeId(graph, "true")
        } else {
            null
        }
        return ExecuteResult(nextNodeId)
    }
}



/**
 * A node that branches based on a user-defined sound.
 */
@Serializable
data class PhonemeMatchNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val rule: VisemeRule? = null,
    override val size: SerializableSize = SerializableSize(250f, 200f),
    override val expandedSize: SerializableSize? = SerializableSize(1000f, 800f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }


    /**
     * Executes the logic for this node.
     * It checks if the incoming audio peaks satisfy all the 'AND' conditions
     * and none of the 'NOT' conditions in the rule.
     */
    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        // If there's no rule or the rule has no conditions, it can't match.
        if (rule?.conditions.isNullOrEmpty()) {
            return ExecuteResult(null)
        }

        // Check if all conditions in the rule are met by the current frequency peaks.
        val allConditionsMet = rule.conditions.all { condition ->
            // Count how many of the current audio peaks fall within this condition's box.
            val hitCount = context.frequencyPeaks.count { (freq, mag) ->
                freq in condition.frequencyRange && mag in condition.magnitudeRange
            }

            // Check the condition based on its type.
            when (condition.type) {
                // For an 'AND' condition, we must have at least the required number of hits.
                ConditionType.AND -> hitCount >= condition.requiredHits
                // For a 'NOT' condition, we must have zero hits.
                ConditionType.NOT -> hitCount == 0
            }
        }

        val nextNodeId = if (allConditionsMet) {
            findNextNodeId(graph, "true")
        } else {
            null
        }
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
    override val size: SerializableSize = SerializableSize(250f, 200f),
    override val expandedSize: SerializableSize? = null
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        if (mode) { // Hold mode
            val nextNodeId = if (context.hotKeyPressed?.shallowEquals(hotkey) == true) {
                findNextNodeId(graph, "true")
            } else {
                null
            }
            return ExecuteResult(nextNodeId)
        } else { // Toggle mode
            val keyWasPressed = context.hotKeyPressed?.shallowEquals(hotkey) == true &&
                    context.hotKeyPressed?.shallowEquals(context.lastProcessedHotkey) != true

            val isCurrentlyOn = context.toggledOnNodes.contains(id)

            val shouldBeOn = if (keyWasPressed) !isCurrentlyOn else isCurrentlyOn

            val nextNodeId = if (shouldBeOn) {
                findNextNodeId(graph, "true")
            } else {
                null
            }

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
        HotKeyNode(id = "", position = SerializableOffset(0f, 0f)),
        PhonemeMatchNode(id = "", position = SerializableOffset(0f, 0f))
    )
}
