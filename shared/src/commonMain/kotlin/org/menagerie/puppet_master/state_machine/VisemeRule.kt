package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

// Represents a single selection box (AND, AND NOT)
@Serializable
data class RuleCondition(
    @Serializable(with = ClosedFloatRangeSerializer::class)
    val frequencyRange: ClosedFloatingPointRange<Float> = 80f..200f,
    @Serializable(with = ClosedFloatRangeSerializer::class)
    val magnitudeRange: ClosedFloatingPointRange<Float> = 0f..1000f,
    val requiredHits: Int = 1,
    val type: ConditionType = ConditionType.AND
)

@Serializable
enum class ConditionType {
    AND,
    NOT
}

// Represents the complete rule for a single viseme
@Serializable
data class VisemeRule(
    val visemeName: String,
    val conditions: List<RuleCondition> = emptyList()
)

enum class DrawMode {
    REPLACE,
    ADD,
    SUBTRACT
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