package org.menagerie.puppet_master

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor

actual fun decodeToImageBitmap(byteArray: ByteArray): ImageBitmap {
    return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size).asImageBitmap()
}

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
