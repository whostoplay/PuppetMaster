package org.menagerie.puppet_master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import kotlin.math.pow

enum class Orientation {
    Vertical,
    Horizontal
}

@Composable
fun VolumeIndicator(
    level: Float, // A value between 0.0 and 1.0
    orientation: Orientation,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    sensitivity: Float = 1.0f, // New sensitivity/gain parameter
    thresholds: Map<Float, PuppetStateInfo?> = emptyMap(),
    onAddThreshold: (Float) -> Unit = {},
    onUpdateThreshold: (oldValue: Float, newValue: Float) -> Unit = { _, _ -> },
    onThresholdSelected: (Float) -> Unit = {}
) {
    // Apply the sensitivity curve to the raw level
    val boostedLevel = (level.pow(0.5f) * sensitivity).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .border(width = 1.dp, color = Color.Gray)
            .pointerInput(orientation) { // Pass orientation to recalculate on change
                detectTapGestures(
                    onDoubleTap = { offset ->
                        val position = when (orientation) {
                            Orientation.Vertical -> 1f - (offset.y / size.height)
                            Orientation.Horizontal -> offset.x / size.width
                        }
                        onAddThreshold(position.coerceIn(0f, 1f))
                    }
                )
            }
    ) {
        Box(
            modifier = Modifier.graphicsLayer {
                clip = true
            }
        ) {
            when (orientation) {
                Orientation.Vertical -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(boostedLevel) // Use the boosted level
                            .background(color)
                    )
                }
                Orientation.Horizontal -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(boostedLevel) // Use the boosted level
                            .background(color)
                    )
                }
            }
        }

        // Threshold markers
        thresholds.entries.forEach { (thresholdValue, stateInfo) ->
            var dragPosition by remember(thresholdValue) { mutableStateOf(thresholdValue) }

            when (orientation) {
                Orientation.Vertical -> {
                    val yOffsetDp = (1f - dragPosition) * maxHeight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp) // Increased touch target
                            .align(Alignment.TopStart)
                            .offset(y = yOffsetDp - 10.dp) // Center the touch target
                            .pointerInput(dragPosition) {
                                detectTapGestures(
                                    onTap = { onThresholdSelected(dragPosition) }
                                )
                            }
                            .draggable(
                                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    dragPosition = (dragPosition - delta / this@BoxWithConstraints.constraints.maxHeight).coerceIn(0f, 1f)
                                },
                                onDragStopped = {
                                    onUpdateThreshold(thresholdValue, dragPosition)
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(Color.Red)
                        )
                        stateInfo?.name?.let {
                            Text(
                                text = it,
                                color = Color.Black,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(start = 4.dp)
                                    .background(Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
                Orientation.Horizontal -> {
                    val xOffsetDp = dragPosition * maxWidth
                    Box(
                        modifier = Modifier
                            .width(20.dp) // Increased touch target
                            .fillMaxHeight()
                            .align(Alignment.TopStart)
                            .offset(x = xOffsetDp - 10.dp) // Center the touch target
                            .pointerInput(dragPosition) {
                                detectTapGestures(
                                    onTap = { onThresholdSelected(dragPosition) }
                                )
                            }
                            .draggable(
                                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    dragPosition = (dragPosition + delta / this@BoxWithConstraints.constraints.maxWidth).coerceIn(0f, 1f)
                                },
                                onDragStopped = {
                                    onUpdateThreshold(thresholdValue, dragPosition)
                                }
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(Color.Red)
                        )
                        stateInfo?.name?.let {
                            Text(
                                text = it,
                                color = Color.Black,
                                fontSize = 10.sp,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 4.dp)
                                    .background(Color.White.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }
        }
    }
}
