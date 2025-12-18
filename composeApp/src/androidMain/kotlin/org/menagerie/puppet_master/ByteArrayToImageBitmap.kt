package org.menagerie.puppet_master

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Converts a [ByteArray] to an [ImageBitmap].
 */
@Composable
actual fun ByteArray.toImageBitmap(): ImageBitmap {
    return BitmapFactory.decodeByteArray(this, 0, this.size).asImageBitmap()
}
