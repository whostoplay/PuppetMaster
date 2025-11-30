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
    thresholds: Map<Float, PuppetStateInfo?> = emptyMap(),
    onAddThreshold: (Float) -> Unit = {},
    onUpdateThreshold: (oldValue: Float, newValue: Float) -> Unit = { _, _ -> },
    onThresholdSelected: (Float) -> Unit = {}
) {
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
                            .fillMaxHeight(level.coerceIn(0f, 1f))
                            .background(color)
                    )
                }
                Orientation.Horizontal -> {
                    Box(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .fillMaxHeight()
                            .fillMaxWidth(level.coerceIn(0f, 1f))
                            .background(color)
                    )
                }
            }
        }

        // Threshold markers
        thresholds.entries.forEach { (thresholdValue, stateInfo) ->
            var currentThreshold by remember(thresholdValue) { mutableStateOf(thresholdValue) }

            when (orientation) {
                Orientation.Vertical -> {
                    val yOffsetDp = (1f - thresholdValue) * maxHeight
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(20.dp) // Increased touch target
                            .align(Alignment.TopStart)
                            .offset(y = yOffsetDp - 10.dp) // Center the touch target
                            .pointerInput(currentThreshold) {
                                detectTapGestures(
                                    onTap = { onThresholdSelected(currentThreshold) }
                                )
                            }
                            .draggable(
                                orientation = androidx.compose.foundation.gestures.Orientation.Vertical,
                                state = rememberDraggableState { delta ->
                                    currentThreshold = (currentThreshold - delta / this@BoxWithConstraints.constraints.maxHeight).coerceIn(0f, 1f)
                                },
                                onDragStopped = {
                                    // When the drag is finished, call onUpdateThreshold to persist the change
                                    onUpdateThreshold(thresholdValue, currentThreshold)
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
                    val xOffsetDp = thresholdValue * maxWidth
                    Box(
                        modifier = Modifier
                            .width(20.dp) // Increased touch target
                            .fillMaxHeight()
                            .align(Alignment.TopStart)
                            .offset(x = xOffsetDp - 10.dp) // Center the touch target
                            .pointerInput(currentThreshold) {
                                detectTapGestures(
                                    onTap = { onThresholdSelected(currentThreshold) }
                                )
                            }
                            .draggable(
                                orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                                state = rememberDraggableState { delta ->
                                    currentThreshold = (currentThreshold + delta / this@BoxWithConstraints.constraints.maxWidth).coerceIn(0f, 1f)
                                },
                                onDragStopped = {
                                    onUpdateThreshold(thresholdValue, currentThreshold)
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
