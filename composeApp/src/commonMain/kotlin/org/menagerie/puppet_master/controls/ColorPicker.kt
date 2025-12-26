package org.menagerie.puppet_master.controls

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.rememberColorMapBitmap

/**
 * A composable that allows the user to pick a color from a color map.
 *
 * A button is displayed, and when clicked, a color map is shown.
 * Tapping on the color map selects a color and invokes the [onColorSelected] callback.
 *
 * @param showColors Whether the color picker is initially visible.
 * @param onColorSelected A callback that is invoked when a color is selected.
 */
@Composable
fun ColorPicker(
    showColors: Boolean = false,
    onColorSelected: (Color) -> Unit
) {
    var showColors by remember { mutableStateOf(showColors) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(
            onClick = { showColors = !showColors },
        ) {
            Text(Strings.getString(Strings.Keys.BG_COLOR_TEXT))
        }
        if (showColors) {
            ColorGrid {
                showColors = false
                onColorSelected(it)
            }

        }
    }
}

@Composable
fun ColorGrid(
    onColorSelected: (Color) -> Unit
) {
    var value by remember{mutableFloatStateOf(1f)}
    val colorMapBitmap = rememberColorMapBitmap(256, 256, value)

    Image(
        bitmap = colorMapBitmap,
        contentDescription = Strings.getString(Strings.Keys.COLOR_MAP_CONTENT_DESCRIPTION),
        modifier = Modifier
            .size(200.dp)
            .padding(top = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = (offset.x / size.width * colorMapBitmap.width).toInt().coerceIn(0, colorMapBitmap.width - 1)
                    val y = (offset.y / size.height * colorMapBitmap.height).toInt().coerceIn(0, colorMapBitmap.height - 1)

                    val hue = (x.toFloat() / colorMapBitmap.width) * 360f
                    val saturation = 1f - (y.toFloat() / colorMapBitmap.height)

                    onColorSelected(Color.hsv(hue, saturation, value))
                }
            }
    )

    Slider(
        modifier = Modifier
            .size(200.dp)
            .padding(top = 8.dp),
        value = value,
        onValueChange = {
            value = it
        },
        valueRange = 0f .. 1f
    )


}