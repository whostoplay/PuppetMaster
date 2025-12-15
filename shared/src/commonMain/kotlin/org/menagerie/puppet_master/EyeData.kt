package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

@Serializable
data class SerializableOffset(val x: Float, val y: Float)

@Serializable
data class Eye(
    val openState: String,
    val pupil: String? = null,
    val closedState: String? = null,
    val position: SerializableOffset = SerializableOffset(0f, 0f),
    val scale: Float = 1f,
    val maxPupilRadiusX: Float = 75f,
    val maxPupilRadiusY: Float = 75f
)

@Serializable
data class EyePair(
    val left: Eye,
    val right: Eye,
    val followCursor: Boolean = false,
    val focusOnGame: Boolean = false,
    val gameScreenLocation: SerializableOffset = SerializableOffset(0.5f, 0.5f),
    val checkOnAudience: Boolean = false,
    val audienceCheckRate: Long = 8000L, // Time between checks
    val audienceCheckDuration: Long = 1500L // How long the check lasts
)

@Serializable
data class EyeState(
    val stateName: String,
    val eyes: EyePair,
    val cursorPosition: SerializableOffset? = null
)
