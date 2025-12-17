package org.menagerie.puppet_master.previews

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
            alignment = Alignment.Center,
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(
                dismissOnClickOutside = false,
                focusable = false
            )
        ) {
            Box(Modifier.size(200.dp)) {
                content()
            }
        }
    }
}
