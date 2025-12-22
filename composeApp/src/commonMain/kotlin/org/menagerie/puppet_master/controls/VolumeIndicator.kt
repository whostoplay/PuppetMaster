package org.menagerie.puppet_master.controls

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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import org.menagerie.puppet_master.PuppetStateInfo
import kotlin.math.pow

/**
 * A composable that displays a horizontal volume level indicator, with a single draggable threshold.
 *
 * @param level The current volume level (a value between 0.0 and 1.0).
 * @param threshold The current threshold value (a value between 0.0 and 1.0).
 * @param onThresholdChange A callback that is invoked when the threshold is moved.
 * @param modifier The modifier to be applied to the indicator.
 * @param color The color of the indicator.
 * @param sensitivity The sensitivity of the indicator. Higher values will make the indicator more responsive to lower volume levels.
 */
@Composable
fun VolumeIndicator(
    level: Float,
    threshold: Float,
    onThresholdChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    sensitivity: Float = 1.0f
) {
    val boostedLevel = (level.pow(0.5f) * sensitivity).coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier.border(width = 1.dp, color = Color.Gray)
    ) {
        Box(
            modifier = Modifier.graphicsLayer { clip = true }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(boostedLevel)
                    .background(color)
            )
        }

        var dragPosition by remember(threshold) { mutableStateOf(threshold) }

        val xOffsetDp = dragPosition * maxWidth
        Box(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight()
                .align(Alignment.TopStart)
                .offset(x = xOffsetDp - 10.dp)
                .draggable(
                    orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        dragPosition = (dragPosition + delta / this@BoxWithConstraints.constraints.maxWidth).coerceIn(0f, 1f)
                    },
                    onDragStopped = { onThresholdChange(dragPosition) }
                )
        ) {
            Box(modifier = Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight().background(Color.Red))
        }
    }
}

/**
 * A composable that displays a horizontal volume level indicator, with draggable thresholds.
 *
 * @param level The current volume level (a value between 0.0 and 1.0).
 * @param modifier The modifier to be applied to the indicator.
 * @param color The color of the indicator.
 * @param sensitivity The sensitivity of the indicator. Higher values will make the indicator more responsive to lower volume levels.
 * @param thresholds A map of threshold values to puppet states. The key is a float between 0.0 and 1.0, representing the threshold position.
 * @param onAddThreshold A callback that is invoked when a new threshold is added (by double-tapping).
 * @param onUpdateThreshold A callback that is invoked when a threshold is moved.
 * @param onThresholdSelected A callback that is invoked when a threshold is tapped.
 */
@Composable
fun VolumeIndicator(
    level: Float,
    modifier: Modifier = Modifier,
    color: Color = Color.Green,
    sensitivity: Float = 1.0f,
    thresholds: Map<Float, PuppetStateInfo?> = emptyMap(),
    onAddThreshold: (Float) -> Unit = {},
    onUpdateThreshold: (oldValue: Float, newValue: Float) -> Unit = { _, _ -> },
    onThresholdSelected: (Float) -> Unit = {}
) {
    val boostedLevel = (level.pow(0.5f) * sensitivity).coerceIn(0f, 1f)

    fun getPosition(offset: Offset, size: IntSize): Float {
        return (offset.x / size.width).coerceIn(0f, 1f)
    }

    BoxWithConstraints(
        modifier = modifier
            .border(width = 1.dp, color = Color.Gray)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset -> onAddThreshold(getPosition(offset, size)) }
                )
            }
    ) {
        Box(
            modifier = Modifier.graphicsLayer { clip = true }
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .fillMaxWidth(boostedLevel)
                    .background(color)
            )
        }

        thresholds.entries.forEach { (thresholdValue, stateInfo) ->
            var dragPosition by remember(thresholdValue) { mutableStateOf(thresholdValue) }

            val xOffsetDp = dragPosition * maxWidth
            Box(
                modifier = Modifier
                    .width(20.dp)
                    .fillMaxHeight()
                    .align(Alignment.TopStart)
                    .offset(x = xOffsetDp - 10.dp)
                    .pointerInput(dragPosition) { detectTapGestures(onTap = { onThresholdSelected(dragPosition) }) }
                    .draggable(
                        orientation = androidx.compose.foundation.gestures.Orientation.Horizontal,
                        state = rememberDraggableState { delta -> dragPosition = (dragPosition + delta / this@BoxWithConstraints.constraints.maxWidth).coerceIn(0f, 1f) },
                        onDragStopped = { onUpdateThreshold(thresholdValue, dragPosition) }
                    )
            ) {
                Box(modifier = Modifier.align(Alignment.Center).width(1.dp).fillMaxHeight().background(Color.Red))
                stateInfo?.name?.let {
                    Text(
                        text = it,
                        color = Color.Black,
                        fontSize = 10.sp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp).background(Color.White.copy(alpha = 0.5f))
                    )
                }
            }
        }
    }
}
