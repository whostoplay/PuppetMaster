package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.runtime.Composable

data class ContextMenuItem(val label: String, val onClick: () -> Unit)

@Composable
expect fun ContextMenuWrapper(
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit
)
