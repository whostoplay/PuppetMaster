package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

/**
 * An `expect` composable for a control drawer.
 * The actual implementation is platform-specific.
 *
 * @param show Whether to show the drawer.
 * @param onDismissRequest Callback for when the drawer is dismissed.
 * @param content The content to display inside the drawer.
 */
@Composable
expect fun ControlDrawer(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)
