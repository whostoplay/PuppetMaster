package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem as FoundationContextMenuItem
import androidx.compose.runtime.Composable

@Composable
actual fun ContextMenuWrapper(
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit
) {
    ContextMenuArea(
        items = {
            items().map { item ->
                FoundationContextMenuItem(item.label, item.onClick)
            }
        },
        content = content
    )
}
