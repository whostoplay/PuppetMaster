package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

/**
 * A composable that shows a control drawer.
 *
 * @param show Whether to show the drawer.
 * @param onDismissRequest The callback that is invoked when the drawer is dismissed.
 * @param content The content to show in the drawer.
 */
@Composable
actual fun ControlDrawer(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {}
