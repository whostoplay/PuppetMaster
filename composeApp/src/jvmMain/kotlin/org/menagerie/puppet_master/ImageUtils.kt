package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

actual fun decodeToImageBitmap(byteArray: ByteArray): ImageBitmap {
    return Image.makeFromEncoded(byteArray).toComposeImageBitmap()
}

/**
 * A composable that remembers a generated color map bitmap.
 *
 * @param width The width of the bitmap to generate.
 * @param height The height of the bitmap to generate.
 * @return The generated [ImageBitmap].
 */
@Composable
actual fun rememberColorMapBitmap(width: Int, height: Int): ImageBitmap {
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
    return decodeToImageBitmap(byteArray)
}