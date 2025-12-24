package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput

@Composable
actual fun ContextMenuWrapper(
    items: () -> List<ContextMenuItem>,
    content: @Composable () -> Unit
) {
    var isContextMenuVisible by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { isContextMenuVisible = true })
        }
    ) {
        content()
        DropdownMenu(
            expanded = isContextMenuVisible,
            onDismissRequest = { isContextMenuVisible = false }
        ) {
            items().forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        item.onClick()
                        isContextMenuVisible = false
                    },
                    text = { Text(item.label) },
                )
            }
        }
    }
}
