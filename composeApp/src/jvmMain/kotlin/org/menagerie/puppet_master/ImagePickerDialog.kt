package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

/**
 * A dialog for picking one or more images from the local file system.
 * This is the JVM implementation which uses Swing's JFileChooser.
 *
 * @param show Whether the dialog should be shown.
 * @param title The title of the dialog.
 * @param multiSelect Whether to allow multiple file selection.
 * @param initialDirectory The initial directory to open the file chooser in.
 * @param onCancel Callback for when the dialog is cancelled.
 * @param onResult Callback for when image(s) are selected, providing a list of pairs containing the image bytes and file name.
 * @param onFolderSelected Callback providing the parent folder of the selected image(s).
 */
@Composable
actual fun ImagePickerDialog(
    show: Boolean,
    title: String,
    multiSelect: Boolean,
    initialDirectory: String?,
    onCancel: () -> Unit,
    onResult: (List<Pair<ByteArray, String>>) -> Unit,
    onFolderSelected: (String) -> Unit
) {
    if (show) {
        val chooser = JFileChooser(initialDirectory?.let { File(it) })
        chooser.dialogTitle = title
        chooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
        chooser.isMultiSelectionEnabled = multiSelect
        val result = chooser.showOpenDialog(null)

        if (result == JFileChooser.APPROVE_OPTION) {
            val files = if (multiSelect) chooser.selectedFiles else arrayOf(chooser.selectedFile)
            if (files.isNotEmpty()) {
                files.first().parent?.let { onFolderSelected(it) }
                val images = files.mapNotNull { file ->
                    if (file.exists()) {
                        file.readBytes() to file.name
                    } else {
                        null
                    }
                }
                onResult(images)
            }
        } else {
            onCancel()
        }
    }
}
