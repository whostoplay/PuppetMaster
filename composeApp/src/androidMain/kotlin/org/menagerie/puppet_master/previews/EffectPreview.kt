package org.menagerie.puppet_master.previews

import androidx.compose.runtime.Composable
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties

@Composable
actual fun EffectPreview(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    if (show) {
        Popup(
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                dismissOnClickOutside = false,
                focusable = false
            )
        ) {
            content()
        }
    }
}
