package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A modifier that combines multiple gestures for manipulating an eye-like UI element.
 *
 * This modifier handles three types of interactions:
 * - **Dragging:** A simple drag gesture to move the element.
 * - **Scaling:** A double-tap followed by a drag gesture to scale the element.
 * - **Radius Change:** A long-press followed by a drag gesture to change the radius of the element.
 *
 * @param onDrag A callback that is invoked when a drag gesture is detected. It provides the drag amount as an [Offset].
 * @param onScale A callback that is invoked when a scale gesture is detected (double-tap and drag). It provides the drag amount as an [Offset] to be used for scaling.
 * @param onRadiusChange A callback that is invoked when a radius change gesture is detected (long-press and drag). It provides the drag amount as an [Offset] to be used for changing the radius.
 * @return A [Modifier] that listens for the combined eye gestures.
 */
actual fun Modifier.combinedEyeGestures(
    onDrag: (dragAmount: Offset) -> Unit,
    onScale: (scaleFactor: Offset) -> Unit,
    onRadiusChange: (dragAmount: Offset) -> Unit
): Modifier = composed {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnScale by rememberUpdatedState(onScale)
    val currentOnRadiusChange by rememberUpdatedState(onRadiusChange)

    var radiusEditMode by remember { mutableStateOf(false) }
    var scaleEditMode by remember { mutableStateOf(false) }

    pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                radiusEditMode = true
            },
            onDoubleTap = {
                scaleEditMode = true
            }
        )
    }.pointerInput(radiusEditMode, scaleEditMode) {
        if (radiusEditMode) {
            detectDragGestures(
                onDragEnd = { radiusEditMode = false },
                onDragCancel = { radiusEditMode = false }
            ) { change, dragAmount ->
                currentOnRadiusChange(dragAmount)
                change.consume()
            }
        } else if (scaleEditMode) {
            detectDragGestures(
                onDragEnd = { scaleEditMode = false },
                onDragCancel = { scaleEditMode = false }
            ) { change, dragAmount ->
                currentOnScale(dragAmount)
                change.consume()
            }
        } else {
            detectDragGestures { change, dragAmount ->
                currentOnDrag(dragAmount)
                change.consume()
            }
        }
    }
}
