package org.menagerie.puppet_master

import android.graphics.BitmapFactory
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import java.io.File

private val httpClient = HttpClient()

/**
 * A composable that remembers and loads an image from a network URL or a local file.
 * Returns null if the image is loading or fails to load.
 *
 * @param url The URL of the image to load. Can be a network URL (e.g., http://...) or a local file URL (e.g., file://...).
 * @return The loaded image as an [ImageBitmap], or null if the image is loading or fails to load.
 */
@Composable
actual fun rememberImageFromUrl(url: String): ImageBitmap? {
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            imageBitmap.value = try {
                val bytes = if (Url(url).protocol.name == "file") {
                    val path = url.removePrefix("file://")
                    File(path).takeIf { it.exists() }?.readBytes()
                } else {
                    httpClient.get(url).body()
                }
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap() }
            } catch (e: Exception) {
                println("Error loading image from url: $url, error: ${e.message}")
                null
            }
        } else {
            imageBitmap.value = null
        }
    }

    return imageBitmap.value
}