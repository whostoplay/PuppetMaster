package org.menagerie.puppet_master

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun ImageFilePicker(buttonText: String, onImagesSelected: (List<Pair<ByteArray, String>>) -> Unit) {
    Button(onClick = { 
        val chooser = JFileChooser()
        chooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
        chooser.isMultiSelectionEnabled = true
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val files = chooser.selectedFiles
            val images = files.map { it.readBytes() to it.name }
            onImagesSelected(images)
        }
    }) {
        Text(buttonText)
    }
}