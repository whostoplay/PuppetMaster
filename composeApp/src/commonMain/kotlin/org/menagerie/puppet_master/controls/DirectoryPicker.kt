package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

@Composable
expect fun DirectoryPicker(
    show: Boolean,
    onDirectorySelected: (String?) -> Unit
)
