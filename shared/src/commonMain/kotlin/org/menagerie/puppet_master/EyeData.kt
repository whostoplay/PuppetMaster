package org.menagerie.puppet_master

import kotlinx.serialization.Serializable

/**
 * A serializable class representing a 2D offset or position.
 *
 * @property x The horizontal component of the offset.
 * @property y The vertical component of the offset.
 */
@Serializable
data class SerializableOffset(val x: Float, val y: Float)

/**
 * Represents a single eye, defining its appearance and behavior.
 *
 * @property openState The name of the puppet state to use when the eye is open.
 * @property pupil An optional name for the pupil image state.
 * @property closedState An optional name for the puppet state to use when the eye is closed (e.g., for blinking).
 * @property position The center position of the eye on the puppet's face, as a normalized offset.
 * @property scaleX The horizontal scale of the eye.
 * @property scaleY The vertical scale of the eye.
 * @property maxPupilRadiusX The maximum horizontal radius the pupil can move from the eye's center.
 * @property maxPupilRadiusY The maximum vertical radius the pupil can move from the eye's center.
 */
@Serializable
data class Eye(
    val openState: String,
    val pupil: String? = null,
    val closedState: String? = null,
    val position: SerializableOffset = SerializableOffset(0f, 0f),
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val maxPupilRadiusX: Float = 75f,
    val maxPupilRadiusY: Float = 75f
)

/**
 * Represents a pair of eyes and their collective behavior.
 *
 * @property left The left eye.
 * @property right The right eye.
 * @property followCursor If true, the pupils will follow the cursor's position.
 * @property focusOnGame If true, the pupils will focus on a specific point on the screen.
 * @property gameScreenLocation The normalized screen coordinates for the pupils to focus on when [focusOnGame] is true.
 * @property checkOnAudience If true, the eyes will periodically look towards the "audience" (i.e., straight ahead).
 * @property audienceCheckRate The time in milliseconds between checks on the audience.
 * @property audienceCheckDuration How long, in milliseconds, the audience check (looking forward) lasts.
 */
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

/**
 * Represents the complete state of the eyes at a given moment, including pupil tracking information.
 *
 * @property stateName The name of this eye state.
 * @property eyes The [EyePair] configuration for this state.
 * @property cursorPosition The last known cursor position, used for pupil tracking.
 */
@Serializable
data class EyeState(
    val stateName: String,
    val eyes: EyePair,
    val cursorPosition: SerializableOffset? = null
)
