package org.menagerie.puppet_master.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.rememberColorMapBitmap

/**
 * A composable that allows the user to pick a color from a color map.
 *
 * @param onColorSelected A callback that is invoked when a color is selected.
 */
@Composable
fun ColorPicker(
    onColorSelected: (Color) -> Unit
) {
    var showColors by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = { showColors = !showColors }
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
            )
        }
    }
}