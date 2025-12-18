package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.isAltPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import kotlinx.coroutines.coroutineScope

/**
 * A modifier that detects combined drag, scale, and radius change gestures for the eyes,
 * customized for desktop (JVM) with keyboard modifier support.
 *
 * This modifier listens for pointer input and interprets it as different gestures based on
 * which keyboard keys are pressed:
 * - **No modifier:** A simple drag gesture, which calls [onDrag] with the drag amount.
 * - **Shift key pressed:** A scaling gesture, which calls [onScale] with the position change.
 * - **Alt key pressed:** A radius change gesture, which calls [onRadiusChange] with the position change.
 *
 * @param onDrag Callback invoked for a standard drag gesture, providing the drag offset.
 * @param onScale Callback invoked for a scaling gesture (when Shift is pressed), providing the change in position.
 * @param onRadiusChange Callback invoked for a radius change gesture (when Alt is pressed), providing the change in position.
 * @return A [Modifier] that listens for these combined gestures.
 */
actual fun Modifier.combinedEyeGestures(
    onDrag: (dragAmount: Offset) -> Unit,
    onScale: (scaleFactor: Offset) -> Unit,
    onRadiusChange: (dragAmount: Offset) -> Unit
): Modifier = composed {
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnScale by rememberUpdatedState(onScale)
    val currentOnRadiusChange by rememberUpdatedState(onRadiusChange)

    pointerInput(Unit) {
        forEachGesture {
            coroutineScope {
                awaitPointerEventScope {
                    val down = awaitPointerEvent()
                    down.changes.forEach { it.consume() } // Consume to prevent other gestures

                    val isShift = down.keyboardModifiers.isShiftPressed
                    val isAlt = down.keyboardModifiers.isAltPressed

                    drag(down.changes.first().id) {
                        when {
                            isShift -> {
                                currentOnScale(it.positionChange())
                            }

                            isAlt -> {
                                currentOnRadiusChange(it.positionChange())
                            }

                            else -> {
                                currentOnDrag(it.positionChange())
                            }
                        }
                        it.consume()
                    }
                }
            }
        }
    }
}
