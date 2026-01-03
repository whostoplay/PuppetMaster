package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    Box {
        content()

        contextMenuPosition?.let { position ->
            Popup(
                offset = IntOffset(position.x.toInt(), position.y.toInt()),
                onDismissRequest = { editorViewModel.closeContextMenu() }
            ) {
                MaterialTheme(
                    colorScheme = colorScheme,
                    typography = typography,
                    shapes = shapes
                ) {
                    Surface(
                        modifier = Modifier.width(250.dp),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        tonalElevation = 3.dp
                    ) {
                        NodePalette(editorViewModel = editorViewModel)
                    }
                }
            }
        }
    }
}
