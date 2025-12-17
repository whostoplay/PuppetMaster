package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.gameScreenGestures(onUpdate: (positionDelta: Offset) -> Unit): Modifier = composed {
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    pointerInput(Unit) {
        detectDragGestures { change, dragAmount ->
            change.consume()
            currentOnUpdate(dragAmount)
        }
    }
}
