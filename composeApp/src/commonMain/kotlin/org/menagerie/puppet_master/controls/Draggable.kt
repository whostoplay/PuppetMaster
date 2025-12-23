package org.menagerie.puppet_master.controls

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.localisation.Strings

/**
 * A composable that can be dragged horizontally to resize adjacent elements.
 *
 * @param onDelta Callback that reports the change in horizontal position.
 * @param modifier Modifier for this composable.
 */
@Composable
fun DraggableSplitter(
    onDelta: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(24.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.background)
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, dragAmount ->
                    change.consume()
                    onDelta(dragAmount)
                }
            }
    ) {
        Icon(
            imageVector = Icons.Default.DragIndicator,
            contentDescription = Strings.getString(Strings.Keys.DRAG_TO_RESIZE_CONTENT_DESCRIPTION),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
