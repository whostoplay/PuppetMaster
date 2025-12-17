package org.menagerie.puppet_master

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput

/**
 * A Modifier that detects when a pointer enters or exits the component's bounds and
 * calls the [onHover] lambda with `true` for enter and `false` for exit.
 *
 * This is useful for tracking hover state to show or hide controls.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onHover(onHover: (Boolean) -> Unit): Modifier = pointerInput(onHover) {
    awaitPointerEventScope {
        while (true) {
            when (awaitPointerEvent().type) {
                PointerEventType.Enter -> onHover(true)
                PointerEventType.Exit -> onHover(false)
            }
        }
    }
}
