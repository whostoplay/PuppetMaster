package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

@Composable
actual fun ImagePickerDialog(
    show: Boolean,
    title: String,
    multiSelect: Boolean,
    onCancel: () -> Unit,
    onResult: (List<Pair<ByteArray, String>>) -> Unit
) {
    // Not implemented for Android yet
}
