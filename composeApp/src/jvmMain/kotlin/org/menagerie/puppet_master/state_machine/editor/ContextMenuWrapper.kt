package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
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
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@Composable
actual fun ContextMenuWrapper(
    editorViewModel: NodeEditorViewModel,
    horizontalScrollState: ScrollState,
    verticalScrollState: ScrollState,
    canvasOffset: IntOffset,
    content: @Composable () -> Unit
) {
    val contextMenuPosition by editorViewModel.contextMenuPosition.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    val shapes = MaterialTheme.shapes

    Box {
        content()

        contextMenuPosition?.let { position ->
            Popup(
                popupPositionProvider = object : PopupPositionProvider {
                    override fun calculatePosition(
                        anchorBounds: IntRect,
                        windowSize: IntSize,
                        layoutDirection: LayoutDirection,
                        popupContentSize: IntSize
                    ): IntOffset {
                        val x = position.x.toInt() - horizontalScrollState.value
                        val y = position.y.toInt() - verticalScrollState.value
                        return canvasOffset + IntOffset(x, y)
                    }
                },
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
