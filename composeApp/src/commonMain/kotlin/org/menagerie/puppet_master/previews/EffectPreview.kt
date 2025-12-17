package org.menagerie.puppet_master.previews

import androidx.compose.runtime.Composable

@Composable
expect fun EffectPreview(
    show: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
)
