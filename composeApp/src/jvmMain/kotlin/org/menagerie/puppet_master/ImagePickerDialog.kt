package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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
