package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun FileSaver(
    show: Boolean,
    defaultFileName: String,
    fileExtensions: List<String>,
    onFileSaved: (String?) -> Unit
) {
    if (show) {
        val fileChooser = JFileChooser().apply {
            selectedFile = java.io.File(defaultFileName)
            fileFilter = FileNameExtensionFilter(fileExtensions.joinToString(", "), *fileExtensions.toTypedArray())
        }
        val result = fileChooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            var file = fileChooser.selectedFile
            if (!file.name.contains(".")) {
                file = java.io.File(file.absolutePath + "." + fileExtensions.first())
            }
            onFileSaved(file.absolutePath)
        } else {
            onFileSaved(null)
        }
    }
}