package org.menagerie.puppet_master

import androidx.compose.ui.geometry.Offset
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure

object OffsetSerializer : KSerializer<Offset> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Offset") {
        element<Float>("x")
        element<Float>("y")
    }

    override fun serialize(encoder: Encoder, value: Offset) {
        encoder.encodeStructure(descriptor) {
            encodeFloatElement(descriptor, 0, value.x)
            encodeFloatElement(descriptor, 1, value.y)
        }
    }

    override fun deserialize(decoder: Decoder): Offset {
        return decoder.decodeStructure(descriptor) {
            var x = 0f
            var y = 0f
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> x = decodeFloatElement(descriptor, 0)
                    1 -> y = decodeFloatElement(descriptor, 1)
                    -1 -> break
                    else -> error("Unexpected index: $index")
                }
            }
            Offset(x, y)
        }
    }
}

@Serializable
data class Eye(
    val openState: String,
    val pupil: String? = null,
    val closedState: String? = null,
    @Serializable(with = OffsetSerializer::class)
    val position: Offset = Offset.Zero,
    val scale: Float = 1f
)

@Serializable
data class EyePair(
    val left: Eye,
    val right: Eye,
    val followCursor: Boolean = false
)

@Serializable
data class EyeState(
    val stateName: String,
    val eyes: EyePair
)
