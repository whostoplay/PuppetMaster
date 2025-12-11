package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.coroutineScope

actual fun Modifier.eyeGestures(onUpdate: (positionDelta: Offset, scaleDelta: Float) -> Unit): Modifier = composed {
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    pointerInput(Unit) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val down = awaitPointerEvent()
                    val isShift = down.keyboardModifiers.isShiftPressed
                    down.changes.forEach { it.consume() }

                    drag(down.changes.first().id) {
                        if (isShift) {
                            // Shift-drag is for scaling
                            currentOnUpdate(Offset.Zero, 1.0f + it.positionChange().y / 100f)
                        } else {
                            // Simple drag is for panning
                            currentOnUpdate(it.positionChange(), 1.0f)
                        }
                        it.consume()
                    }
                }
            }
        }
    }
}

actual fun Modifier.radiusGestures(onUpdate: (positionDelta: Offset, scaleDelta: Offset) -> Unit): Modifier = composed {
    val currentOnUpdate by rememberUpdatedState(onUpdate)
    pointerInput(Unit) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val down = awaitPointerEvent()
                    val isShift = down.keyboardModifiers.isShiftPressed
                    down.changes.forEach { it.consume() }

                    drag(down.changes.first().id) {
                        if (isShift) {
                            currentOnUpdate(Offset.Zero, it.positionChange())
                        } else {
                            currentOnUpdate(it.positionChange(), Offset.Zero)
                        }
                        it.consume()
                    }
                }
            }
        }
    }
}
