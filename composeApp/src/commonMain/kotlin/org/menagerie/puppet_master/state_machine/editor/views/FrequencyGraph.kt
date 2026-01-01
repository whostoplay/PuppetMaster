package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.log10
import kotlin.math.pow

private const val MAX_MAGNITUDE = 30000f
private const val SAMPLE_RATE = 16000f
private const val MIN_FREQUENCY_HZ = 80f // Start plot at a reasonable low-end for human voice
private const val MAX_FREQUENCY_HZ = SAMPLE_RATE / 2f

@Composable
fun FrequencyGraph(
    modifier: Modifier = Modifier,
    frequencyData: FloatArray,
    selectedFrequencyRange: ClosedFloatingPointRange<Float>,
    selectedMagnitudeRange: ClosedFloatingPointRange<Float>,
    onAreaSelected: (frequencyRange: ClosedFloatingPointRange<Float>, magnitudeRange: ClosedFloatingPointRange<Float>) -> Unit,
    onPeaksDetected: (peaks: List<Pair<Float, Float>>) -> Unit
) {
    var startDrag by remember { mutableStateOf<Offset?>(null) }
    var currentRect by remember { mutableStateOf<Rect?>(null) }
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    val currentPeaks = remember { mutableStateListOf<Pair<Offset, Boolean>>() }

    LaunchedEffect(frequencyData) {
        if (frequencyData.isNotEmpty() && componentSize != IntSize.Zero) {
            val canvasWidth = componentSize.width.toFloat()
            val canvasHeight = componentSize.height.toFloat()

            val minLogFreq = log10(MIN_FREQUENCY_HZ)
            val maxLogFreq = log10(MAX_FREQUENCY_HZ)
            val logFreqRange = maxLogFreq - minLogFreq

            val allSignificantPeaks = mutableListOf<Pair<Float, Float>>()
            val newPeaksWithSelection = mutableListOf<Pair<Offset, Boolean>>()

            val startingBin = (MIN_FREQUENCY_HZ / (SAMPLE_RATE / 2) * frequencyData.size).toInt().coerceAtLeast(4)

            for (index in startingBin until frequencyData.size - 4) {
                val magnitude = frequencyData[index]

                val isPeak = magnitude > frequencyData[index - 1] &&
                        magnitude > frequencyData[index + 1] &&
                        magnitude > frequencyData[index - 2] &&
                        magnitude > frequencyData[index + 2] &&
                        magnitude > frequencyData[index - 3] &&
                        magnitude > frequencyData[index + 3] &&
                        magnitude > frequencyData[index - 4] &&
                        magnitude > frequencyData[index + 4]

                if (isPeak && magnitude > 1500) { // Even more drastically stricter Peak detection
                    val frequency = index * (SAMPLE_RATE / 2) / frequencyData.size
                    allSignificantPeaks.add(Pair(frequency, magnitude))

                    // Convert frequency to log scale for X coordinate
                    if (frequency > 0) {
                        val logFrequency = log10(frequency)
                        val x = ((logFrequency - minLogFreq) / logFreqRange) * canvasWidth
                        val y = canvasHeight - (magnitude / MAX_MAGNITUDE * canvasHeight).coerceIn(0f, canvasHeight)

                        val isSelected = frequency in selectedFrequencyRange && magnitude in selectedMagnitudeRange
                        newPeaksWithSelection.add(Offset(x, y) to isSelected)
                    }
                }
            }
            onPeaksDetected(allSignificantPeaks)

            // Only show the current frame's peaks
            currentPeaks.clear()
            currentPeaks.addAll(newPeaksWithSelection)
        }
    }

    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        startDrag = offset
                    },
                    onDrag = { change, _ ->
                        startDrag?.let {
                            currentRect = Rect(it, change.position)
                        }
                    },
                    onDragEnd = {
                        currentRect?.let {
                            val normalizedRect = it.normalize()
                            val canvasWidth = componentSize.width.toFloat()
                            val canvasHeight = componentSize.height.toFloat()

                            // Convert X from log screen space back to frequency
                            val minLogFreq = log10(MIN_FREQUENCY_HZ)
                            val maxLogFreq = log10(MAX_FREQUENCY_HZ)
                            val logFreqRange = maxLogFreq - minLogFreq

                            val logFreqStart = minLogFreq + (normalizedRect.left / canvasWidth) * logFreqRange
                            val logFreqEnd = minLogFreq + (normalizedRect.right / canvasWidth) * logFreqRange

                            val frequencyStart = 10f.pow(logFreqStart)
                            val frequencyEnd = 10f.pow(logFreqEnd)

                            val magnitudeStart = ((canvasHeight - normalizedRect.bottom) / canvasHeight) * MAX_MAGNITUDE
                            val magnitudeEnd = ((canvasHeight - normalizedRect.top) / canvasHeight) * MAX_MAGNITUDE

                            onAreaSelected(frequencyStart..frequencyEnd, magnitudeStart..magnitudeEnd)
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
            listOf(100f, 200f, 500f, 1000f, 2000f, 5000f).forEach { freq ->
                if (freq >= MIN_FREQUENCY_HZ && freq <= MAX_FREQUENCY_HZ) {
                    val x = ((log10(freq) - minLogFreq) / logFreqRange) * canvasWidth
                    drawLine(gridColor, Offset(x, 0f), Offset(x, canvasHeight), 1f)
                }
            }

            // Draw the current peaks
            if (currentPeaks.isNotEmpty()) {
                drawPoints(
                    points = currentPeaks.filter { !it.second }.map { it.first },
                    pointMode = PointMode.Points,
                    color = Color.Gray,
                    strokeWidth = 2.dp.toPx()
                )
                drawPoints(
                    points = currentPeaks.filter { it.second }.map { it.first },
                    pointMode = PointMode.Points,
                    color = Color.Green,
                    strokeWidth = 3.dp.toPx()
                )
            }

            // Draw the committed selection rectangle on a log scale
            val committedLeft = ((log10(selectedFrequencyRange.start.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth
            val committedRight = ((log10(selectedFrequencyRange.endInclusive.coerceAtLeast(MIN_FREQUENCY_HZ)) - minLogFreq) / logFreqRange) * canvasWidth

            val committedRect = Rect(
                left = committedLeft.toFloat(),
                right = committedRight.toFloat(),
                top = canvasHeight - (selectedMagnitudeRange.endInclusive / MAX_MAGNITUDE) * canvasHeight,
                bottom = canvasHeight - (selectedMagnitudeRange.start / MAX_MAGNITUDE) * canvasHeight
            )
            drawRect(
                color = Color.White.copy(alpha = 0.5f),
                topLeft = committedRect.topLeft,
                size = committedRect.size,
                style = Stroke(width = 1.dp.toPx())
            )

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
