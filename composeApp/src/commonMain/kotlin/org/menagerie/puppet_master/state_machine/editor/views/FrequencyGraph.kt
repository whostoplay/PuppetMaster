package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.menagerie.puppet_master.state_machine.ConditionType
import org.menagerie.puppet_master.state_machine.DrawMode
import org.menagerie.puppet_master.state_machine.VisemeRule
import org.menagerie.puppet_master.state_machine.editor.viewmodels.PhonemeViewModel
import kotlin.math.log10

private const val MAX_MAGNITUDE = 30000f
private const val SAMPLE_RATE = 16000f
private const val MIN_FREQUENCY_HZ = 80f // Start plot at a reasonable low-end for human voice
private const val MAX_FREQUENCY_HZ = SAMPLE_RATE / 2f

enum class PeakStatus { Normal, InAndBox, InNotBox }

@Composable
fun FrequencyGraph(
    modifier: Modifier = Modifier,
    frequencyData: FloatArray,
    rule: VisemeRule?,
    onRuleChanged: (VisemeRule) -> Unit,
    onPeaksDetected: (peaks: List<Pair<Float, Float>>) -> Unit,
    drawMode: DrawMode,
    selectedConditionIndex: Int?,
    onConditionSelected: (Int?) -> Unit,
    viewModel: PhonemeViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var startDrag by remember { mutableStateOf<Offset?>(null) }
    var currentRect by remember { mutableStateOf<Rect?>(null) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(rule) {
        rule?.let { viewModel.onRuleChanged(it) }
    }

    LaunchedEffect(uiState.rule) {
        uiState.rule?.let { onRuleChanged(it) }
    }

    LaunchedEffect(selectedConditionIndex) {
        viewModel.onConditionSelected(selectedConditionIndex)
    }

    LaunchedEffect(uiState.allSignificantPeaks) {
        onPeaksDetected(uiState.allSignificantPeaks)
    }

    LaunchedEffect(frequencyData, componentSize) {
        viewModel.updateFrequencyAnalysis(frequencyData, componentSize)
    }

    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .pointerInput(uiState.rule, drawMode) { // Depend on rule and drawMode
                // DETECTOR 1: For handling taps to select conditions
                detectTapGestures(
                    onTap = { offset: Offset ->
                        val canvasWidth = componentSize.width.toFloat()
                        val canvasHeight = componentSize.height.toFloat()
                        val minLogFreq = log10(MIN_FREQUENCY_HZ)
                        val logFreqRange = log10(MAX_FREQUENCY_HZ) - minLogFreq

                        val clickedConditionIndex = uiState.rule?.conditions?.indexOfLast { condition ->
                            val left = ((log10(condition.frequencyRange.start.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                            val right = ((log10(condition.frequencyRange.endInclusive.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                            val top = canvasHeight - (condition.magnitudeRange.endInclusive / MAX_MAGNITUDE) * canvasHeight
                            val bottom = canvasHeight - (condition.magnitudeRange.start / MAX_MAGNITUDE) * canvasHeight
                            val conditionRect = Rect(left, top, right, bottom)
                            conditionRect.contains(offset)
                        }?.takeIf { it != -1 }

                        onConditionSelected(clickedConditionIndex)
                    }
                )
            }
            .pointerInput(uiState.rule, drawMode) { // DETECTOR 2: For handling drag gestures to draw
                detectDragGestures(
                    onDragStart = { offset: Offset ->
                        onConditionSelected(null) // Deselect any condition when starting a new drag
                        startDrag = offset
                    },
                    onDrag = { change, _ ->
                        startDrag?.let {
                            currentRect = Rect(it, change.position)
                        }
                    },
                    onDragEnd = {
                        currentRect?.let {
                            viewModel.handleDragEnd(it, drawMode, componentSize)
                        }
                        startDrag = null
                        currentRect = null
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            // Draw grid
            val gridColor = Color.DarkGray
            val horizontalLines = 10
            val horizontalSpacing = canvasHeight / horizontalLines
            for (i in 1..horizontalLines) {
                drawLine(gridColor, Offset(0f, i * horizontalSpacing), Offset(canvasWidth, i * horizontalSpacing), 1f)
            }

            val minLogFreq = log10(MIN_FREQUENCY_HZ)
            val maxLogFreq = log10(MAX_FREQUENCY_HZ)
            val logFreqRange = maxLogFreq - minLogFreq
            listOf(100f, 200f, 500f, 1000f, 2000f, 5000f, 10000f).forEach { freq ->
                if (freq >= MIN_FREQUENCY_HZ && freq <= MAX_FREQUENCY_HZ) {
                    val x = ((log10(freq) - minLogFreq) / logFreqRange) * canvasWidth
                    drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), 1f)
                    val text = if (freq < 1000) "${freq.toInt()}" else "${(freq / 1000).toInt()}k"
                    val style = TextStyle(
                        color = gridColor,
                        fontSize = 12.sp
                    )
                    val textLayoutResult = textMeasurer.measure(text, style)
                    drawText(
                        textLayoutResult,
                        topLeft = Offset(x - textLayoutResult.size.width / 2, canvasHeight - textLayoutResult.size.height)
                    )
                }
            }

            // Draw the current peaks
            if (uiState.peaks.isNotEmpty()) {
                // Draw normal (unselected) peaks first
                drawPoints(
                    points = uiState.peaks.filter { it.second == PeakStatus.Normal }.map { it.first },
                    pointMode = PointMode.Points,
                    color = Color.Gray,
                    strokeWidth = 2.dp.toPx()
                )

                // --- NEW: Draw 'AND' hits as a connected path ---
                val andHitPoints = uiState.peaks.filter { it.second == PeakStatus.InAndBox }.map { it.first }
                if (andHitPoints.isNotEmpty()) {
                    val andPath = Path().apply {
                        // Start the path at the first hit
                        moveTo(andHitPoints.first().x, andHitPoints.first().y)
                        // Draw a line to each subsequent hit
                        for (i in 1 until andHitPoints.size) {
                            lineTo(andHitPoints[i].x, andHitPoints[i].y)
                        }
                    }
                    // Draw the path with a thick green stroke, but no fill
                    drawPath(
                        path = andPath,
                        color = Color.Green,
                        style = Stroke(width = 3.dp.toPx())
                    )
                }

                // Draw 'NOT' hits as prominent magenta dots
                drawPoints(
                    points = uiState.peaks.filter { it.second == PeakStatus.InNotBox }.map { it.first },
                    pointMode = PointMode.Points,
                    color = Color(0xFFFF00FF), // Magenta - better contrast
                    strokeWidth = 5.dp.toPx(), // Make them pop
                    cap = StrokeCap.Round // Use round caps for a softer dot
                )
            }

            // Draw the committed selection rectangle on a log scale
            uiState.rule?.conditions?.forEachIndexed { index, condition ->
                val left = ((log10(condition.frequencyRange.start.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                val right = ((log10(condition.frequencyRange.endInclusive.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                val top = canvasHeight - (condition.magnitudeRange.endInclusive / MAX_MAGNITUDE) * canvasHeight
                val bottom = canvasHeight - (condition.magnitudeRange.start / MAX_MAGNITUDE) * canvasHeight
                val conditionRect = Rect(left, top, right, bottom)

                val isSelected = index == uiState.selectedConditionIndex
                val (color, strokeWidth) = when {
                    isSelected -> when (condition.type) {
                        ConditionType.AND -> Color.Green to 4.dp.toPx()
                        ConditionType.NOT -> Color(0xFFFF00FF) to 4.dp.toPx()
                    }
                    else -> when (condition.type) {
                        ConditionType.AND -> Color.Green.copy(alpha = 0.5f) to 2.dp.toPx()
                        ConditionType.NOT -> Color(0xFFFF00FF).copy(alpha = 0.5f) to 2.dp.toPx()
                    }
                }

                drawRect(
                    color = color,
                    topLeft = conditionRect.topLeft,
                    size = conditionRect.size,
                    style = Stroke(width = strokeWidth)
                )
            }

            currentRect?.let {
                val color = when (drawMode) {
                    DrawMode.ADD -> Color.Green.copy(alpha = 0.5f)
                    DrawMode.SUBTRACT -> Color.Red.copy(alpha = 0.5f)
                    DrawMode.REPLACE -> Color.Blue.copy(alpha = 0.5f)
                }
                drawRect(
                    color = color,
                    topLeft = it.normalize().topLeft,
                    size = it.normalize().size
                )
            }
        }
    }
}

fun Rect.normalize(): Rect {
    return Rect(
        left = minOf(left, right),
        top = minOf(top, bottom),
        right = maxOf(left, right),
        bottom = maxOf(top, bottom)
    )
}
