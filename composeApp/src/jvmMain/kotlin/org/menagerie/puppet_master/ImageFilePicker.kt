package org.menagerie.puppet_master

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
actual fun ImageFilePicker(onImageSelected: (ByteArray, String) -> Unit) {
    Button(onClick = { 
        val chooser = JFileChooser()
        chooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val file = chooser.selectedFile
            onImageSelected(file.readBytes(), file.name)
        }
    }) {
        Text("Select Image")
    }
}