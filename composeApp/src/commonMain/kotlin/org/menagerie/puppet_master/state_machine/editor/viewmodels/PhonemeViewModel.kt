package org.menagerie.puppet_master.state_machine.editor.viewmodels

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.menagerie.puppet_master.state_machine.ConditionType
import org.menagerie.puppet_master.state_machine.DrawMode
import org.menagerie.puppet_master.state_machine.RuleCondition
import org.menagerie.puppet_master.state_machine.VisemeRule
import org.menagerie.puppet_master.state_machine.editor.views.PeakStatus
import org.menagerie.puppet_master.state_machine.editor.views.normalize
import kotlin.math.log10
import kotlin.math.pow

private const val MAX_MAGNITUDE = 30000f
private const val SAMPLE_RATE = 16000f
private const val MIN_FREQUENCY_HZ = 80f
private const val MAX_FREQUENCY_HZ = SAMPLE_RATE / 2f

data class PhonemeUiState(
    val rule: VisemeRule? = null,
    val peaks: List<Pair<Offset, PeakStatus>> = emptyList(),
    val allSignificantPeaks: List<Pair<Float, Float>> = emptyList(),
    val selectedConditionIndex: Int? = null
)

class PhonemeViewModel {

    private val _uiState = MutableStateFlow(PhonemeUiState())
    val uiState: StateFlow<PhonemeUiState> = _uiState.asStateFlow()

    fun onRuleChanged(rule: VisemeRule) {
        _uiState.update { it.copy(rule = rule) }
    }

    fun onConditionSelected(index: Int?) {
        _uiState.update { it.copy(selectedConditionIndex = index) }
    }

    fun handleDragEnd(rect: Rect, drawMode: DrawMode, componentSize: IntSize) {
        val rule = _uiState.value.rule
        if (rule == null && (drawMode == DrawMode.ADD || drawMode == DrawMode.SUBTRACT)) {
            return
        }

        val normalizedRect = rect.normalize()
        val canvasWidth = componentSize.width.toFloat()
        val canvasHeight = componentSize.height.toFloat()
        val minLogFreq = log10(MIN_FREQUENCY_HZ)
        val maxLogFreq = log10(MAX_FREQUENCY_HZ)
        val logFreqRange = maxLogFreq - minLogFreq
        val logFreqStart = minLogFreq + (normalizedRect.left / canvasWidth) * logFreqRange
        val logFreqEnd = minLogFreq + (normalizedRect.right / canvasWidth) * logFreqRange
        val frequencyStart = 10f.pow(logFreqStart)
        val frequencyEnd = 10f.pow(logFreqEnd)
        val magnitudeStart = ((canvasHeight - normalizedRect.bottom) / canvasHeight) * MAX_MAGNITUDE
        val magnitudeEnd = ((canvasHeight - normalizedRect.top) / canvasHeight) * MAX_MAGNITUDE

        val newCondition = RuleCondition(
            frequencyRange = frequencyStart..frequencyEnd,
            magnitudeRange = magnitudeStart..magnitudeEnd,
            type = if (drawMode == DrawMode.SUBTRACT) ConditionType.NOT else ConditionType.AND
        )

        val baseRule = rule ?: VisemeRule(visemeName = "Unnamed")

        val updatedRule = when (drawMode) {
            DrawMode.REPLACE -> baseRule.copy(conditions = listOf(newCondition))
            DrawMode.ADD, DrawMode.SUBTRACT -> baseRule.copy(conditions = baseRule.conditions + newCondition)
        }
        _uiState.update { it.copy(rule = updatedRule) }
    }

    fun updateFrequencyAnalysis(
        frequencyData: FloatArray,
        componentSize: IntSize,
    ) {
        if (frequencyData.isEmpty() || componentSize == IntSize.Zero) return

        val canvasWidth = componentSize.width.toFloat()
        val canvasHeight = componentSize.height.toFloat()

        val minLogFreq = log10(MIN_FREQUENCY_HZ)
        val maxLogFreq = log10(MAX_FREQUENCY_HZ)
        val logFreqRange = maxLogFreq - minLogFreq

        val allSignificantPeaks = mutableListOf<Pair<Float, Float>>()
        val newPeaksWithStatus = mutableListOf<Pair<Offset, PeakStatus>>()

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
                allSignificantPeaks.add(Pair(frequency, magnitude))

                if (frequency > 0) {
                    val logFrequency = log10(frequency)
                    val x = ((logFrequency - minLogFreq) / logFreqRange) * canvasWidth
                    val y = canvasHeight - (magnitude / MAX_MAGNITUDE * canvasHeight).coerceIn(0f, canvasHeight)


                    val peakStatus = _uiState.value.rule?.conditions?.let { conditions ->
                        fun getConditionArea(condition: RuleCondition): Float {
                            val logFreqSpan = log10(condition.frequencyRange.endInclusive) - log10(condition.frequencyRange.start)
                            val magnitudeSpan = condition.magnitudeRange.endInclusive - condition.magnitudeRange.start
                            return logFreqSpan * magnitudeSpan
                        }

                        val containingAnds = conditions.filter {
                            it.type == ConditionType.AND && frequency in it.frequencyRange && magnitude in it.magnitudeRange
                        }
                        val containingNots = conditions.filter {
                            it.type == ConditionType.NOT && frequency in it.frequencyRange && magnitude in it.magnitudeRange
                        }

                        when {
                            containingAnds.isNotEmpty() && containingNots.isNotEmpty() -> {
                                val smallestAndArea = containingAnds.minOf { getConditionArea(it) }
                                val smallestNotArea = containingNots.minOf { getConditionArea(it) }

                                if (smallestAndArea < smallestNotArea) {
                                    PeakStatus.InAndBox
                                } else {
                                    PeakStatus.InNotBox
                                }
                            }
                            containingAnds.isNotEmpty() -> PeakStatus.InAndBox
                            containingNots.isNotEmpty() -> PeakStatus.InNotBox
                            else -> PeakStatus.Normal
                        }
                    } ?: PeakStatus.Normal
                    newPeaksWithStatus.add(Offset(x, y) to peakStatus)
                }
            }
        }
        _uiState.update { it.copy(peaks = newPeaksWithStatus, allSignificantPeaks = allSignificantPeaks) }
    }
}
