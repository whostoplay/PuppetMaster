package org.menagerie.puppet_master.previews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.SERVER_PORT
import org.menagerie.puppet_master.rememberImageFromUrl

@Composable
fun LivePreview(
    operatingMode: OperatingMode,
    displayedImageName: String?,
    uploadsDir: String,
    backgroundColor: Color
) {
    Box(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = if (operatingMode == OperatingMode.ONLINE) {
            "http://127.0.0.1:$SERVER_PORT/uploads/${displayedImageName ?: ""}"
        } else {
            if (displayedImageName?.isNotBlank() == true) "file://$uploadsDir/$displayedImageName" else ""
        }
        val image = rememberImageFromUrl(imageUrl)

        if (image != null) {
            Image(bitmap = image, contentDescription = "Live Preview")
        } else {
            Text("No Active Image")
        }
    }
}
