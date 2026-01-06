package org.menagerie.puppet_master.state_machine

import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.SerializableSize
import kotlin.math.pow

private const val SAMPLE_RATE = 16000f
private const val AUDIO_BUFFER_SIZE = 2048f // Note: This should match AudioProcessor's buffer size for accurate timing
private const val MIN_FREQUENCY_HZ = 80f

@Serializable
data class SpikeDetection(
    val enabled: Boolean = false,
    val threshold: Float = 0.2f, // How much the volume must increase to be considered a spike
    val window: Int = 5 // Over how many recent volume samples to check for a spike
)

/**
 * A node that branches based on the audio volume.
 */
@Serializable
data class VolumeThresholdNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val threshold: Float = 0.5f,
    val volumeGain: Float = 1.0f,
    val spikeDetection: SpikeDetection = SpikeDetection(),
    override val size: SerializableSize = SerializableSize(200f, 210f),
    override val expandedSize: SerializableSize? = SerializableSize(300f, 600f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val volumeHistory = context.volumeHistory[id] ?: emptyList()
        val trigger = if (spikeDetection.enabled) {
            if (volumeHistory.size >= spikeDetection.window) {
                val recentMax = volumeHistory.takeLast(spikeDetection.window).maxOrNull() ?: 0f
                (context.microphoneVolume - recentMax) * volumeGain >= spikeDetection.threshold
            } else {
                false
            }
        } else {
            context.microphoneVolume.pow(0.5f) * volumeGain > threshold
        }

        val nextNodeId = if (trigger) {
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
    val triggerThreshold: Int = 10,
    val confidenceDecayRate: Int = 3,
    override val size: SerializableSize = SerializableSize(250f, 200f),
    override val expandedSize: SerializableSize? = SerializableSize(1000f, 800f)
) : ConditionalNode {

    fun processFrequencyData(frequencyData: FloatArray): List<Pair<Float, Float>> {
        val peaks = mutableListOf<Pair<Float, Float>>()
        if (frequencyData.isEmpty()) {
            return peaks
        }

        val startingBin = (MIN_FREQUENCY_HZ / (SAMPLE_RATE / 2) * frequencyData.size).toInt().coerceAtLeast(4)

        for (index in startingBin until frequencyData.size - 4) {
            val magnitude = frequencyData[index]

            val isPeak = magnitude > 1500 &&
                    magnitude > frequencyData[index - 1] &&
                    magnitude > frequencyData[index + 1] &&
                    magnitude > frequencyData[index - 2] &&
                    magnitude > frequencyData[index + 2] &&
                    magnitude > frequencyData[index - 3] &&
                    magnitude > frequencyData[index + 3] &&
                    magnitude > frequencyData[index - 4] &&
                    magnitude > frequencyData[index + 4]

            if (isPeak) {
                val frequency = index * (SAMPLE_RATE / 2) / frequencyData.size
                if (frequency > 0) {
                    peaks.add(Pair(frequency, magnitude))
                }
            }
        }
        return peaks
    }

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val currentState = context.phonemeMatchStates[id] ?: PhonemeMatchState()

        if (rule?.conditions.isNullOrEmpty()) {
            return ExecuteResult(null, if (currentState.confidence != 0 || currentState.isActive) {
                GraphAction.UpdatePhonemeMatchState(id, PhonemeMatchState(0, false))
            } else {
                null
            })
        }

        val significantPeaks = processFrequencyData(context.frequencyData)

        val allConditionsMet = rule.conditions.all { condition ->
            val hitCount = significantPeaks.count { (freq, mag) ->
                freq in condition.frequencyRange && mag in condition.magnitudeRange
            }
            when (condition.type) {
                ConditionType.AND -> hitCount >= condition.requiredHits
                ConditionType.NOT -> hitCount <= condition.requiredHits
            }
        }

        val newConfidence = if (allConditionsMet) {
            (currentState.confidence + 1).coerceAtMost(triggerThreshold)
        } else {
            (currentState.confidence - confidenceDecayRate).coerceAtLeast(0)
        }

        val newIsActive = if (newConfidence >= triggerThreshold) {
            true
        } else if (newConfidence == 0) {
            false
        } else {
            currentState.isActive
        }

        val action = if (newConfidence != currentState.confidence || newIsActive != currentState.isActive) {
            GraphAction.UpdatePhonemeMatchState(id, PhonemeMatchState(newConfidence, newIsActive))
        } else {
            null
        }

        val nextNodeId = if (newIsActive) {
            findNextNodeId(graph, "true")
        } else {
            null
        }

        return ExecuteResult(nextNodeId, action)
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

@Serializable
data class BeatDetection(
    val enabled: Boolean = true,
    val bpm: Float = 120f,
    val tolerance: Float = 0.2f, // 20% tolerance in timing
    val requiredBeats: Int = 3, // a run of 3 beats to trigger
    val memoryFrames: Int = 40, // about 5 seconds of history
    val spikeThreshold: Float = 0.1f, // volume increase needed to be a spike
    val spikeWindow: Int = 3 // samples to look back for spike detection
)

/**
 * A node that branches based on a detected rhythm in the audio input.
 */
@Serializable
data class RhythmNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val beatDetection: BeatDetection = BeatDetection(),
    override val size: SerializableSize = SerializableSize(250f, 200f),
    override val expandedSize: SerializableSize? = SerializableSize(300f, 350f)
) : ConditionalNode {

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        if (!beatDetection.enabled) {
            return ExecuteResult(null)
        }

        val history = context.volumeHistory[id] ?: emptyList()

        if (history.size < beatDetection.spikeWindow) { // Need a reasonable amount of history
            return ExecuteResult(null)
        }

        // 1. Detect onsets (spikes) in the volume history
        val spikeIndices = mutableListOf<Int>()
        for (i in beatDetection.spikeWindow until history.size) {
            val lookBehind = history.subList(i - beatDetection.spikeWindow, i)
            if (lookBehind.isEmpty()) continue
            val recentMax = lookBehind.maxOrNull() ?: 0f

            if (history[i] > recentMax + beatDetection.spikeThreshold) {
                // To avoid detecting multiple frames for the same beat, we check if the last detected spike is too close.
                if (spikeIndices.isEmpty() || i - spikeIndices.last() > beatDetection.spikeWindow) {
                    spikeIndices.add(i)
                }
            }
        }

        if (spikeIndices.size < beatDetection.requiredBeats) {
            return ExecuteResult(null)
        }

        // 2. Check if the intervals between recent spikes match the BPM
        val recentSpikes = spikeIndices.takeLast(beatDetection.requiredBeats)
        val intervals = recentSpikes.zipWithNext { a, b -> b - a }

        val framesPerSecond = SAMPLE_RATE / AUDIO_BUFFER_SIZE
        val expectedFrameInterval = (60.0 / beatDetection.bpm) * framesPerSecond
        val lowerBound = expectedFrameInterval * (1 - beatDetection.tolerance)
        val upperBound = expectedFrameInterval * (1 + beatDetection.tolerance)

        val rhythmDetected = intervals.all { it.toDouble() in lowerBound..upperBound }

        val nextNodeId = if (rhythmDetected) {
            findNextNodeId(graph, "true")
        } else {
            null
        }

        return ExecuteResult(nextNodeId)
    }
}


/**
 * Provides a list of all available conditional nodes for the palette.
 */
fun getAvailableConditionalNodes(): List<Node> {
    return listOf(
        VolumeThresholdNode(id = "", position = SerializableOffset(0f, 0f)),
        HotKeyNode(id = "", position = SerializableOffset(0f, 0f)),
        PhonemeMatchNode(id = "", position = SerializableOffset(0f, 0f)),
        RhythmNode(id = "", position = SerializableOffset(0f, 0f))
    )
}
