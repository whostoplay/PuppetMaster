package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

@Composable
expect fun FileSaver(
    show: Boolean,
    defaultFileName: String,
    fileExtensions: List<String>,
    onFileSaved: (String?) -> Unit
)
