package org.menagerie.puppet_master.controls

import androidx.compose.runtime.Composable
import javax.swing.JFileChooser

@Composable
actual fun DirectoryPicker(
    show: Boolean,
    onDirectorySelected: (String?) -> Unit
) {
    if (show) {
        val fileChooser = JFileChooser().apply {
            fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
        }
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            onDirectorySelected(fileChooser.selectedFile.absolutePath)
        } else {
            onDirectorySelected(null)
        }
    }
}