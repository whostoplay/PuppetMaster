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

/**
 * A composable that displays a live preview of the puppet.
 *
 * @param operatingMode The current operating mode.
 * @param displayedImageName The name of the image to display.
 * @param uploadsDir The directory where uploaded images are stored.
 * @param backgroundColor The background color of the preview.
 * @param serverIp The IP address of the server.
 */
@Composable
fun LivePreview(
    operatingMode: OperatingMode,
    displayedImageName: String?,
    uploadsDir: String,
    backgroundColor: Color,
    serverIp: String
) {
    Box(
        modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        val imageUrl = when {
            displayedImageName.isNullOrBlank() -> ""
            operatingMode == OperatingMode.ONLINE -> "http://$serverIp:$SERVER_PORT/uploads/$displayedImageName"
            else -> "file://$uploadsDir/$displayedImageName"
        }
        val image = rememberImageFromUrl(imageUrl)

        if (image != null) {
            Image(bitmap = image, contentDescription = "Live Preview")
        } else {
            Text("No Active Image")
        }
    }
}
