package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

/**
 * A composable that allows the user to pick a file.
 * This is an `expect` function, so the actual implementation is platform-specific.
 *
 * @param show Whether to show the file picker.
 * @param fileExtensions A list of allowed file extensions to filter by.
 * @param onFileSelected Callback with the path of the selected file, or null if the operation was cancelled.
 */
@Composable
expect fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (String?) -> Unit
)
