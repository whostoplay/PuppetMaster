package org.menagerie.puppet_master.previews

import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.rememberWindowState

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
