package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

@Composable
expect fun ImageFilePicker(buttonText: String, onImageSelected: (ByteArray, String) -> Unit)
