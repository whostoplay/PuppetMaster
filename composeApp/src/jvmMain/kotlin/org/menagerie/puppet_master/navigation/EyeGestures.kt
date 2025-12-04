package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.coroutineScope

actual fun Modifier.eyeGestures(onUpdate: (positionDelta: Offset, scaleDelta: Float) -> Unit): Modifier {
    return pointerInput(Unit) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val down = awaitPointerEvent()
                    val isAlt = down.keyboardModifiers.isAltPressed
                    down.changes.forEach { it.consume() }

                    drag(down.changes.first().id) {
                        if (isAlt) {
                            onUpdate(Offset.Zero, it.positionChange().y / 100f)
                        } else {
                            onUpdate(it.positionChange(), 0f)
                        }
                        it.consume()
                    }
                }
            }
        }
    }
}
