package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.IntOffset

@Composable
expect fun ContextMenuWrapper(
    editorViewModel: NodeEditorViewModel,
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
    canvasOffset: IntOffset,
    content: @Composable () -> Unit
)
