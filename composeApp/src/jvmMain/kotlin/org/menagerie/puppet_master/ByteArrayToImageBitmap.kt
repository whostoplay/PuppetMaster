package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import org.jetbrains.skia.Image

/**
 * Converts a [ByteArray] into an [ImageBitmap].
 *
 * This is the JVM-specific implementation that uses Skia to decode the byte array.
 *
 * @return The resulting [ImageBitmap].
 */
@Composable
actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return Image.makeFromEncoded(this).toComposeImageBitmap()
}
