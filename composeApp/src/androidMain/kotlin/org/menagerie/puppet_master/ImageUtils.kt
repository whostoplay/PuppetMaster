package org.menagerie.puppet_master

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor

/**
 * Decodes a [ByteArray] into an [ImageBitmap].
 * @param byteArray The byte array to decode.
 * @return The decoded [ImageBitmap].
 */
actual fun decodeToImageBitmap(byteArray: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).asImageBitmap()
}

/**
 * Creates and remembers a color map [ImageBitmap].
 * The bitmap displays hue on the x-axis and saturation on the y-axis.
 *
 * @param width The width of the bitmap.
 * @param height The height of the bitmap.
 * @return The remembered [ImageBitmap].
 */
@Composable
actual fun rememberColorMapBitmap(width: Int, height: Int): ImageBitmap {
    val bitmap = remember(width, height) {
        val buffer = IntArray(width * height)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val hue = (x.toFloat() / width) * 360f
                val saturation = 1f - (y.toFloat() / height)
                buffer[y * width + x] = AndroidColor.HSVToColor(floatArrayOf(hue, saturation, 1f))
            }
        }
        Bitmap.createBitmap(buffer, width, height, Bitmap.Config.ARGB_8888)
    }
    return bitmap.asImageBitmap()
}
