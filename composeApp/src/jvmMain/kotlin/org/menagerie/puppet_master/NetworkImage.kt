package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.CancellationException
import org.jetbrains.skia.Image
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val httpClient = HttpClient()
private val imageCache = ConcurrentHashMap<String, ImageBitmap>()

/**
 * A composable that remembers and loads an image from a network URL or a local file.
 * Returns null if the image is loading or fails to load.
 *
 * @param url The URL of the image to load. Can be a network URL (e.g., http://...) or a local file URL (e.g., file://...).
 * @return The loaded image as an [ImageBitmap], or null if the image is loading or fails to load.
 */
@Composable
actual fun rememberImageFromUrl(url: String): ImageBitmap? {
    val imageBitmap = remember(url) { mutableStateOf(imageCache[url]) }

    LaunchedEffect(url) {
        if (url.isNotBlank() && imageBitmap.value == null) {
            val loadedImage = try {
                val bytes = if (Url(url).protocol.name == "file") {
                    val path = url.removePrefix("file://")
                    File(path).takeIf { it.exists() }?.readBytes()
                } else {
                    httpClient.get(url).body<ByteArray>()
                }
                bytes?.let { Image.makeFromEncoded(it).toComposeImageBitmap() }
            } catch (e: CancellationException) {
                // Image loading was cancelled. This is normal.
                null
            } catch (e: Exception) {
                println("Error loading image from url: $url, error: ${e.message}")
                null
            }

            if (loadedImage != null) {
                imageCache[url] = loadedImage
                imageBitmap.value = loadedImage
            }
        } else if (url.isBlank()) {
            imageBitmap.value = null
        }
    }

    return imageBitmap.value
}
