package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * A composable that shows a file picker dialog.
 *
 * @param show Whether to show the dialog.
 * @param fileExtensions The allowed file extensions.
 * @param onFileSelected The callback that is invoked when a file is selected, or null if the dialog is canceled.
 */
@Composable
actual fun FilePicker(
    show: Boolean,
    fileExtensions: List<String>,
    onFileSelected: (String?) -> Unit
) {
    if (show) {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter(fileExtensions.joinToString(", "), *fileExtensions.toTypedArray())
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onFileSelected(fileChooser.selectedFile.absolutePath)
        } else {
            onFileSelected(null)
        }
    }
}
