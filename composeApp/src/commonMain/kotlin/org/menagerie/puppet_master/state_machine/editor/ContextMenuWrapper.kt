package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.runtime.Composable

@Composable
expect fun ContextMenuWrapper(
    editorViewModel: NodeEditorViewModel,
    content: @Composable () -> Unit
)
