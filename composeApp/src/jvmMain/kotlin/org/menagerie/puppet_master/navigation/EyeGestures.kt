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
                                // Using Y-axis for scaling up/down
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