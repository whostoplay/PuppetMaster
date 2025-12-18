package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A modifier that detects drag gestures on the game screen.
 *
 * This modifier consumes the drag events and reports the drag amount through the [onUpdate] callback.
 *
 * @param onUpdate A callback that is invoked when a drag gesture is detected. It provides the drag amount as an [Offset].
 * @return A [Modifier] that listens for drag gestures.
 */
actual fun Modifier.gameScreenGestures(onUpdate: (positionDelta: Offset) -> Unit): Modifier = composed {
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            currentOnUpdate(dragAmount)
        }
    }
}
