package org.menagerie.puppet_master.controls

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable

/**
 * A modal bottom sheet that displays controls for the application.
 *
 * @param show Whether to show the bottom sheet.
 * @param onDismissRequest Called when the user requests to dismiss the bottom sheet.
 * @param content The content to display inside the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
actual fun ControlDrawer(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    if (show) {
        ModalBottomSheet(
            onDismissRequest = onDismissRequest,
            sheetState = sheetState,
        ) {
            content()
        }
    }
}
