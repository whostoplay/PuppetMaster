package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Applies drag gesture detection to the composable.
 * This is the JVM-specific implementation for game screen gestures.
 *
 * It detects drag gestures and reports the drag amount to the [onUpdate] callback.
 *
 * @param onUpdate Callback invoked when a drag gesture occurs, providing the change in position (delta).
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
