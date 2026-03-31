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
    val bpm: Float = 120f,
    val tolerance: Float = 0.35f,
    val requiredBeats: Int = 4,
    val memoryFrames: Int = 40, // about 5 seconds of history\
    val onsetFactor: Float = 1.60f, // Drastically raised to reject noise
    val minEnergy: Float = 50000f
)

/**
 * A node that branches based on a detected rhythm in the audio input.
 * This version uses full-spectrum energy analysis to detect onsets (beats).
 */
@Serializable
data class RhythmNode(
    override val id: NodeId,
    override val position: SerializableOffset,
    override val branchPriority: Int = 0,
    val beatDetection: BeatDetection = BeatDetection(),
    override val size: SerializableSize = SerializableSize(250f, 210f),
    override val expandedSize: SerializableSize? = SerializableSize(300f, 600f)
) : ConditionalNode {

    @kotlinx.serialization.Transient
    private val energyHistory = mutableListOf<Float>()

    @kotlinx.serialization.Transient
    private var decayCounter = 0

    override fun copyNode(id: NodeId, position: SerializableOffset): Node = this.copy(id = id, position = position)

    override fun copyNodeWithNewPriority(priority: Int): Node {
        return this.copy(branchPriority = priority)
    }

    override fun execute(context: GraphExecutionContext, graph: NodeGraph): ExecuteResult {
        val frequencyData = context.frequencyData

        // --- DYNAMIC TIMING SETUP ---
        val framesPerSecond = SAMPLE_RATE / AUDIO_BUFFER_SIZE
        val expectedFrameInterval = (60.0 / beatDetection.bpm) * framesPerSecond
        val dynamicOnsetWindow = (expectedFrameInterval / 2).toInt().coerceIn(2, 10)
        val dynamicDebounce = (expectedFrameInterval / 4).toInt().coerceIn(2, 10)

        // 1. Energy Check & History Update
        val currentEnergy = frequencyData.sum()

        // Silence/Low-noise gate
        if (currentEnergy < beatDetection.minEnergy) {
            decayCounter = (decayCounter - 1).coerceAtLeast(0)
            return ExecuteResult(if (decayCounter >= beatDetection.requiredBeats) findNextNodeId(graph, "true") else null)
        }

        energyHistory.add(currentEnergy)
        if (energyHistory.size > beatDetection.memoryFrames) energyHistory.removeAt(0)
        if (energyHistory.size < dynamicOnsetWindow) return ExecuteResult(null)

        // 2. Normalize & Detect Onsets
        val maxHistoricalEnergy = energyHistory.maxOrNull()?.coerceAtLeast(1f) ?: 1f
        val normalizedHistory = energyHistory.map { it / maxHistoricalEnergy }
        val spikeIndices = mutableListOf<Int>()

        for (i in dynamicOnsetWindow until normalizedHistory.size) {
            val recentAverage = normalizedHistory.subList(i - dynamicOnsetWindow, i).average().toFloat().coerceAtLeast(0.001f)
            if (normalizedHistory[i] > recentAverage * beatDetection.onsetFactor) {
                if (spikeIndices.isEmpty() || i - spikeIndices.last() > dynamicDebounce) {
                    spikeIndices.add(i)
                }
            }
        }

        // 3. Tempo Analysis
        var rhythmFoundThisTick = false
        var dominantBpm = 0f
        var topBpmHits = 0
        var debugTopThree = ""

        if (spikeIndices.size >= 2) {
            val allIntervals = spikeIndices.zipWithNext { a, b -> b - a }.filter { it > 2 }
            if (allIntervals.isNotEmpty()) {
                val intervalCounts = allIntervals.groupingBy { it }.eachCount()
                val dominantInterval = intervalCounts.maxByOrNull { it.value }?.key ?: 0

                val sortedBpms = intervalCounts.map { (interval, hits) ->
                    (60.0 / (interval / framesPerSecond)).toFloat() to hits
                }.sortedByDescending { it.second }

                if (dominantInterval > 0) {
                    dominantBpm = (60.0 / (dominantInterval / framesPerSecond)).toFloat()
                    val target = beatDetection.bpm
                    rhythmFoundThisTick = isBpmMatch(dominantBpm, target, beatDetection.tolerance) ||
                            isBpmMatch(dominantBpm, target * 2, beatDetection.tolerance) ||
                            isBpmMatch(dominantBpm, target / 2, beatDetection.tolerance)

                    topBpmHits = intervalCounts[dominantInterval] ?: 0
                    debugTopThree = sortedBpms.take(3).joinToString { "[%.0f: %d]".format(it.first, it.second) }
                }
            }
        }

        // 4. Confidence/Decay
        if (rhythmFoundThisTick) {
            decayCounter = (decayCounter + 2).coerceAtMost(10)
        } else {
            decayCounter = (decayCounter - 1).coerceAtLeast(0)
        }

        val isBeatActive = decayCounter >= beatDetection.requiredBeats

        // --- CLEAN LOGGING ---
        if (spikeIndices.size > 0) {
            val status = if (rhythmFoundThisTick) "MATCH" else "SEARCHING"
            println("Rhythm [Target: ${beatDetection.bpm.toInt()}] -> Confidence: $decayCounter/${beatDetection.requiredBeats} | Spikes: ${spikeIndices.size} | Dominant: %.1f BPM (%d hits) | Top: $debugTopThree | $status".format(dominantBpm, topBpmHits))
        }

        return ExecuteResult(if (isBeatActive) findNextNodeId(graph, "true") else null)
    }


    private fun isBpmMatch(detected: Float, target: Float, tolerance: Float): Boolean {
        if (target <= 0 || detected <= 0) return false
        val lowerBound = target * (1 - tolerance)
        val upperBound = target * (1 + tolerance)
        return detected in lowerBound..upperBound
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
