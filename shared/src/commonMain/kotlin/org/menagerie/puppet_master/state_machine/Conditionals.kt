package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
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

object ClosedFloatRangeSerializer : KSerializer<ClosedFloatingPointRange<Float>> {
    @Serializable
    @SerialName("ClosedFloatRange")
    private data class ClosedFloatRangeSurrogate(val start: Float, val endInclusive: Float)

    override val descriptor: SerialDescriptor = ClosedFloatRangeSurrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ClosedFloatingPointRange<Float>) {
        val surrogate = ClosedFloatRangeSurrogate(value.start, value.endInclusive)
        encoder.encodeSerializableValue(ClosedFloatRangeSurrogate.serializer(), surrogate)
    }

    override fun deserialize(decoder: Decoder): ClosedFloatingPointRange<Float> {
        val surrogate = decoder.decodeSerializableValue(ClosedFloatRangeSurrogate.serializer())
        return surrogate.start..surrogate.endInclusive
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
    @Serializable(with = ClosedFloatRangeSerializer::class)
    val frequencyRange: ClosedFloatingPointRange<Float> = 0f..0f,
    @Serializable(with = ClosedFloatRangeSerializer::class)
    val magnitudeRange: ClosedFloatingPointRange<Float> = 0f..0f,
    val minPeaks: Int = 8,
    override val size: SerializableSize = SerializableSize(250f, 200f),
    override val expandedSize: SerializableSize? = SerializableSize(600f, 600f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val hitCount = context.frequencyPeaks.count { (freq, mag) ->
            freq in frequencyRange && mag in magnitudeRange
        }

        println(hitCount)

        val nextNodeId = if (hitCount >= minPeaks) {
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
