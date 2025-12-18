package org.menagerie.puppet_master.previews

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState

/**
 * Displays a preview of an effect in a separate, undecorated, always-on-top window.
 * This is the JVM-specific implementation.
 *
 * @param show Controls the visibility of the preview window.
 * @param onDismissRequest Callback invoked when the user attempts to close the window.
 * @param content The composable content to be displayed within the preview window.
 */
@Composable
actual fun EffectPreview(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val windowState = rememberWindowState(width = 300.dp, height = 300.dp)

    if (show) {
        Window(
            onCloseRequest = onDismissRequest,
            title = "Effect Preview",
            state = windowState,
            undecorated = true,
            alwaysOnTop = true,
        ) {
            WindowDraggableArea {
                content()
            }
        }
    }
}
