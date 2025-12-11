package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.eyeGestures(onUpdate: (positionDelta: Offset, scaleDelta: Float) -> Unit): Modifier = composed {
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            currentOnUpdate(pan, zoom)
        }
    }
}

actual fun Modifier.radiusGestures(onUpdate: (positionDelta: Offset, scaleDelta: Offset) -> Unit): Modifier = composed {
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    pointerInput(Unit) {
        detectDragGestures {
            change, dragAmount ->
            change.consume()
            currentOnUpdate(dragAmount, Offset.Zero)
        }
    }
}