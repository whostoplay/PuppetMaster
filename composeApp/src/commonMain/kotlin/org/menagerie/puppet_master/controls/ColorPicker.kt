package org.menagerie.puppet_master.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.toImageBitmap
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@Composable
private fun rememberColorMapBitmap(width: Int, height: Int): ImageBitmap {
    val byteArray = remember(width, height) {
        val buffer = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val hue = (x.toFloat() / width) * 360f
                val saturation = 1f - (y.toFloat() / height)
                buffer[y * width + x] = Color.hsv(hue, saturation, 1f).toArgb()
            }
        }

        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        bufferedImage.setRGB(0, 0, width, height, buffer, 0, width)

        val outputStream = ByteArrayOutputStream()
        ImageIO.write(bufferedImage, "png", outputStream)
        outputStream.toByteArray()
    }
    return byteArray.toImageBitmap()
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ColorPicker(
    onColorSelected: (Color) -> Unit,
    onHover: (Boolean) -> Unit
) {
    var showColors by remember { mutableStateOf(false) }
    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { showColors = !showColors },
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["colorPickerButton"] = true }
                .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["colorPickerButton"] = false }
        ) {
            Text("BG Color")
        }
        if (showColors) {
            val colorMapBitmap = rememberColorMapBitmap(256, 256)

            Image(
                bitmap = colorMapBitmap,
                contentDescription = "Color Map",
                modifier = Modifier
                    .size(200.dp)
                    .padding(top = 8.dp)
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val x = (offset.x / size.width * colorMapBitmap.width).toInt().coerceIn(0, colorMapBitmap.width - 1)
                            val y = (offset.y / size.height * colorMapBitmap.height).toInt().coerceIn(0, colorMapBitmap.height - 1)

                            val hue = (x.toFloat() / colorMapBitmap.width) * 360f
                            val saturation = 1f - (y.toFloat() / colorMapBitmap.height)

                            onColorSelected(Color.hsv(hue, saturation, 1f))
                            showColors = false
                        }
                    }
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["colorMap"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["colorMap"] = false }
            )
        }
    }
}
