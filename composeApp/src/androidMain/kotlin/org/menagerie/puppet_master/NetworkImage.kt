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
import java.io.File

@Composable
actual fun rememberImageFromUrl(url: String): ImageBitmap? {
    val imageBitmap = remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        if (url.isNotBlank()) {
            imageBitmap.value = try {
                val bytes = if (url.startsWith("file://")) {
                    val path = url.removePrefix("file://")
                    val file = File(path)
                    if (file.exists()) {
                        file.readBytes()
                    } else {
                        null
                    }
                } else {
                    val client = HttpClient()
                    client.get(url).body()
                }
                
                bytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size).asImageBitmap() }
            } catch (e: Exception) {
                // Handle exceptions
                null
            }
        } else {
            imageBitmap.value = null
        }
    }

    return imageBitmap.value
}