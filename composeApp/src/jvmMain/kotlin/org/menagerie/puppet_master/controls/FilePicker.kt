package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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