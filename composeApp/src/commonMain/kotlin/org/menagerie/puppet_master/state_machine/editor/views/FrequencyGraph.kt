package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.input.pointer.PointerInputChange
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
import org.menagerie.puppet_master.state_machine.RuleCondition
import org.menagerie.puppet_master.state_machine.VisemeRule
import kotlin.io.path.moveTo
import kotlin.math.log10
import kotlin.math.pow

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
    onConditionSelected: (Int?) -> Unit
) {
    var startDrag by remember { mutableStateOf<Offset?>(null) }
    var currentRect by remember { mutableStateOf<Rect?>(null) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val textMeasurer = rememberTextMeasurer()

    val currentPeaks = remember { mutableStateListOf<Pair<Offset, PeakStatus>>() }

    LaunchedEffect(frequencyData, rule) { // Add `rule` as a key to re-evaluate when it changes
        if (frequencyData.isNotEmpty() && componentSize != IntSize.Zero) {
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

                // Stricter Peak detection
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


                        // --- "Smaller Box Wins" Intersection Logic ---
                        val peakStatus = rule?.conditions?.let { conditions ->
                            // Helper to calculate the area of a condition box. Using log scale for frequency.
                            fun getConditionArea(condition: RuleCondition): Float {
                                val logFreqSpan = log10(condition.frequencyRange.endInclusive) - log10(condition.frequencyRange.start)
                                val magnitudeSpan = condition.magnitudeRange.endInclusive - condition.magnitudeRange.start
                                return logFreqSpan * magnitudeSpan
                            }

                            // Find all AND and NOT boxes that this peak falls into
                            val containingAnds = conditions.filter {
                                it.type == ConditionType.AND && frequency in it.frequencyRange && magnitude in it.magnitudeRange
                            }
                            val containingNots = conditions.filter {
                                it.type == ConditionType.NOT && frequency in it.frequencyRange && magnitude in it.magnitudeRange
                            }

                            when {
                                // Case 1: Intersection found - The core of the new logic
                                containingAnds.isNotEmpty() && containingNots.isNotEmpty() -> {
                                    // Find the area of the smallest AND box and the smallest NOT box
                                    val smallestAndArea = containingAnds.minOf { getConditionArea(it) }
                                    val smallestNotArea = containingNots.minOf { getConditionArea(it) }

                                    // The box with the smaller area wins
                                    if (smallestAndArea < smallestNotArea) {
                                        PeakStatus.InAndBox
                                    } else {
                                        PeakStatus.InNotBox
                                    }
                                }

                                // Case 2: Only in AND boxes
                                containingAnds.isNotEmpty() -> PeakStatus.InAndBox

                                // Case 3: Only in NOT boxes
                                containingNots.isNotEmpty() -> PeakStatus.InNotBox

                                // Case 4: In no boxes
                                else -> PeakStatus.Normal
                            }
                        } ?: PeakStatus.Normal // If there's no rule, status is Normal

                        newPeaksWithStatus.add(Offset(x, y) to peakStatus)


                        newPeaksWithStatus.add(Offset(x, y) to peakStatus)
                    }
                }
            }
            onPeaksDetected(allSignificantPeaks)

            currentPeaks.clear()
            currentPeaks.addAll(newPeaksWithStatus)
        }
    }


    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .pointerInput(rule, drawMode) { // Depend on rule and drawMode
                // DETECTOR 1: For handling taps to select conditions
                detectTapGestures(
                    onTap = { offset: Offset ->
                        val canvasWidth = componentSize.width.toFloat()
                        val canvasHeight = componentSize.height.toFloat()
                        val minLogFreq = log10(MIN_FREQUENCY_HZ)
                        val logFreqRange = log10(MAX_FREQUENCY_HZ) - minLogFreq

                        val clickedConditionIndex = rule?.conditions?.indexOfLast { condition ->
                            val left = ((log10(condition.frequencyRange.start.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                            val right = ((log10(condition.frequencyRange.endInclusive.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                            val top = canvasHeight - (condition.magnitudeRange.endInclusive / MAX_MAGNITUDE) * canvasHeight
                            val bottom = canvasHeight - (condition.magnitudeRange.start / MAX_MAGNITUDE) * canvasHeight
                            val conditionRect = Rect(left.toFloat(), top, right.toFloat(), bottom)
                            conditionRect.contains(offset)
                        }?.takeIf { it != -1 }

                        onConditionSelected(clickedConditionIndex)
                    }
                )
            }
            .pointerInput(rule, drawMode) { // DETECTOR 2: For handling drag gestures to draw
                detectDragGestures(
                    onDragStart = { offset: Offset ->
                        onConditionSelected(null) // Deselect any condition when starting a new drag
                        startDrag = offset
                    },
                    onDrag = { change: PointerInputChange, _: Offset ->
                        startDrag?.let {
                            currentRect = Rect(it, change.position)
                        }
                    },
                    onDragEnd = {
                        // The onDragEnd logic remains exactly the same as before
                        currentRect?.let { rect ->
                            if (rule == null && (drawMode == DrawMode.ADD || drawMode == DrawMode.SUBTRACT)) {
                                startDrag = null
                                currentRect = null
                                return@let // Use return@let to exit the let block
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

                            onRuleChanged(updatedRule)
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
            if (currentPeaks.isNotEmpty()) {
                // Draw normal (unselected) peaks first
                drawPoints(
                    points = currentPeaks.filter { it.second == PeakStatus.Normal }.map { it.first },
                    pointMode = PointMode.Points,
                    color = Color.Gray,
                    strokeWidth = 2.dp.toPx()
                )

                // --- NEW: Draw 'AND' hits as a connected path ---
                val andHitPoints = currentPeaks.filter { it.second == PeakStatus.InAndBox }.map { it.first }
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
                    points = currentPeaks.filter { it.second == PeakStatus.InNotBox }.map { it.first },
                    pointMode = PointMode.Points,
                    color = Color(0xFFFF00FF), // Magenta - better contrast
                    strokeWidth = 5.dp.toPx(), // Make them pop
                    cap = StrokeCap.Round // Use round caps for a softer dot
                )
            }



            // Draw the committed selection rectangle on a log scale
            rule?.conditions?.forEachIndexed { index, condition ->
                val committedLeft =
                    ((log10(condition.frequencyRange.start.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
                val committedRight =
                    ((log10(condition.frequencyRange.endInclusive.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth

                // Convert magnitude range to Y coordinates
                val committedTop =
                    canvasHeight - (condition.magnitudeRange.endInclusive / MAX_MAGNITUDE) * canvasHeight
                val committedBottom =
                    canvasHeight - (condition.magnitudeRange.start / MAX_MAGNITUDE) * canvasHeight

                val committedRect = Rect(
                    left = committedLeft.toFloat(),
                    right = committedRight.toFloat(),
                    top = committedTop,
                    bottom = committedBottom
                )
                val color = when (condition.type) {
                    ConditionType.AND -> Color.White.copy(alpha = 0.5f)
                    ConditionType.NOT -> Color.Red.copy(alpha = 0.4f)
                }

                val isSelected = selectedConditionIndex == index

                drawRect(
                    color = color,
                    topLeft = committedRect.topLeft,
                    size = committedRect.size,
                    style = Stroke(width = if (isSelected) 3.dp.toPx() else 1.dp.toPx())
                )
            }

            // Draw the current dragging selection rectangle
            currentRect?.let {
                drawRect(
                    color = Color.White,
                    topLeft = it.topLeft,
                    size = it.size,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

private fun Rect.normalize(): Rect {
    return Rect(
        left = minOf(left, right),
        top = minOf(top, bottom),
        right = maxOf(left, right),
        bottom = maxOf(top, bottom)
    )
}
