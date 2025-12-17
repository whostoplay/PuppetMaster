package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

@Composable
expect fun ImageFilePicker(
    buttonText: String,
    initialDirectory: String?,
    onImagesSelected: (List<Pair<ByteArray, String>>) -> Unit,
    onFolderSelected: (String) -> Unit
)
