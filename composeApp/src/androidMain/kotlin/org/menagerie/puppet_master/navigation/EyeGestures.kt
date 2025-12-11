package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.combinedEyeGestures(
    onDrag: (dragAmount: Offset) -> Unit,
    onScale: (scaleFactor: Float) -> Unit,
    onRadiusChange: (dragAmount: Offset) -> Unit
): Modifier = composed {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnScale by rememberUpdatedState(onScale)
    val currentOnRadiusChange by rememberUpdatedState(onRadiusChange)

    var radiusEditMode by remember { mutableStateOf(false) }

    val inputModifier = if (radiusEditMode) {
        Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragEnd = { radiusEditMode = false },
                onDragCancel = { radiusEditMode = false }
            ) { change, dragAmount ->
                currentOnRadiusChange(dragAmount)
                change.consume()
            }
        }
    } else {
        Modifier.pointerInput(Unit) {
            detectTransformGestures { _, pan, zoom, _ ->
                currentOnDrag(pan)
                currentOnScale(zoom)
            }
        }
    }

    inputModifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                radiusEditMode = true
            }
        )
    }
}
