package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

@Composable
expect fun ControlDrawer(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)
