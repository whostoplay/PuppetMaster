package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput

actual fun Modifier.eyeGestures(position: Offset, scale: Float, onUpdate: (Offset, Float) -> Unit): Modifier {
    return pointerInput(Unit) {
        detectTransformGestures { _, pan, zoom, _ ->
            onUpdate(position + pan, scale * zoom)
        }
    }
}
