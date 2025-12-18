package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable

/**
 * A composable that allows the user to save a file.
 * This is an `expect` function, so the actual implementation is platform-specific.
 *
 * @param show Whether to show the file saver.
 * @param defaultFileName The default file name.
 * @param fileExtensions The allowed file extensions.
 * @param onFileSaved Callback with the path of the saved file, or null if the operation was cancelled.
 */
@Composable
expect fun FileSaver(
    show: Boolean,
    defaultFileName: String,
    fileExtensions: List<String>,
    onFileSaved: (String?) -> Unit
)
