package org.menagerie.puppet_master.previews

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.ActiveSpecialEffect
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
 * @param activeSpecialEffect The currently active special effect.
 */
@Composable
fun LivePreview(
    operatingMode: OperatingMode,
    displayedImageName: String?,
    uploadsDir: String,
    backgroundColor: Color,
    serverIp: String,
    activeSpecialEffect: ActiveSpecialEffect?
) {
    var frame by remember { mutableLongStateOf(0L) }

    LaunchedEffect(activeSpecialEffect) {
        if (activeSpecialEffect != null) {
            while (true) {
                frame = System.currentTimeMillis()
                delay(16) // roughly 60 fps
            }
        }
    }

    BoxWithConstraints(
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
            val offset = activeSpecialEffect?.getVibrationOffset(maxWidth.value / 20f)

            Image(
                bitmap = image,
                contentDescription = "Live Preview",
                colorFilter = activeSpecialEffect?.getGlow()?.let { ColorFilter.lighting(Color.White, Color(it, it, it)) },
                modifier = Modifier
                    .scale(activeSpecialEffect?.getScaleX() ?: 1f, activeSpecialEffect?.getScaleY() ?: 1f)
                    .graphicsLayer(rotationZ = activeSpecialEffect?.getRotation() ?: 0f)
                    .offset((offset?.x ?: 0f).dp, (offset?.y ?: 0f).dp)
                    .let { if (frame > 0) it else it } // force recomposition
            )
        } else {
            Text("No Active Image")
        }
    }
}
