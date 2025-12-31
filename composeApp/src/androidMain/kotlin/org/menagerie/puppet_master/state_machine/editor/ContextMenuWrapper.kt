package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup

@Composable
actual fun ContextMenuWrapper(
    editorViewModel: NodeEditorViewModel,
    content: @Composable () -> Unit) {
    val contextMenuPosition by editorViewModel.contextMenuPosition.collectAsState()

    Box {
        content()

        contextMenuPosition?.let { position ->
            Popup(
                offset = IntOffset(position.x.toInt(), position.y.toInt()),
                onDismissRequest = { editorViewModel.closeContextMenu() }
            ) {
                Column(
                    modifier = Modifier
                        .width(250.dp)
                        .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                ) {
                    NodePalette(editorViewModel = editorViewModel)
                }
            }
        }
    }
}
