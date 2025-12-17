package org.menagerie.puppet_master

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.model.ImageRequest
import com.seiko.imageloader.model.ImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A composable that remembers and loads an image from a network URL or a local file.
 * Returns null if the image is loading or fails to load.
 *
 * @param url The URL of the image to load. Can be a network URL (e.g., http://...) or a local file URL (e.g., file://...).
 * @return The loaded image as an [ImageBitmap], or null if the image is loading or fails to load.
 */
@Composable
actual fun rememberImageFromUrl(url: String): ImageBitmap? {
    var imageBitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    val imageLoader = LocalImageLoader.current

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            val newBitmap = withContext(Dispatchers.IO) {
                val request = ImageRequest(url)
                try {
                    when (val result = imageLoader.execute(request)) {
                        is ImageResult.Bitmap -> result.bitmap.asImageBitmap()
                        is ImageResult.Image -> (result.image as? BitmapDrawable)?.bitmap?.asImageBitmap()
                        else -> null
                    }
                } catch (e: Exception) {
                    println("Error loading image from url: $url, error: ${e.message}")
                    null
                }
            }
            imageBitmap = newBitmap
        } else {
            imageBitmap = null
        }
    }

    return imageBitmap
}