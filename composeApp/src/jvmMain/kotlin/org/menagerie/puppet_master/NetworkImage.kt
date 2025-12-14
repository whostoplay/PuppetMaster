package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.cache.disk.DiskCache
import com.seiko.imageloader.cache.memory.MemoryCache
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.decoder.SkiaImageDecoder
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.intercept.DiskCacheInterceptor
import com.seiko.imageloader.intercept.MemoryCacheInterceptor
import com.seiko.imageloader.model.ImageRequest
import com.seiko.imageloader.model.ImageResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path.Companion.toOkioPath
import org.jetbrains.skia.Image
import java.io.File

private val imageLoader = ImageLoader {
    components {
        setupDefaultComponents()
        add(SkiaImageDecoder.Factory())
    }
    interceptor {
        addInterceptor(MemoryCacheInterceptor {
            MemoryCache {
                maxSizePercent(0.25)
            }
        })
        addInterceptor(DiskCacheInterceptor {
            DiskCache {
                directory(File(System.getProperty("java.io.tmpdir"), "image_cache").toOkioPath())
                maxSizeBytes(512L * 1024 * 1024)
            }
        })
    }
}


/**
 * A composable that remembers and loads an image from a network URL or a local file.
 * Returns null if the image is loading or fails to load.
 *
 * @param url The URL of the image to load. Can be a network URL (e.g., http://...) or a local file URL (e.g., file://...).
 * @return The loaded image as an [ImageBitmap], or null if the image is loading or fails to load.
 */
@Composable
actual fun rememberImageFromUrl(url: String, forceReloadKey: Any?): ImageBitmap? {
    val imageBitmap by produceState<ImageBitmap?>(initialValue = null, key1 = url, key2 = forceReloadKey) {
        value = if (url.isNotBlank()) {
            withContext(Dispatchers.IO) {
                try {
                    val finalUrl = if (url.startsWith("http")) {
                        "$url?cache_bust=${forceReloadKey?.hashCode() ?: System.currentTimeMillis()}"
                    } else {
                        url
                    }
                    val request = ImageRequest(finalUrl.removePrefix("file://"))
                    when (val result = imageLoader.execute(request)) {
                        is ImageResult.Image -> {
                            result.image.toComposeImageBitmap()
                        }
                        is ImageResult.Bitmap -> {
                            Image.makeFromBitmap(result.bitmap).toComposeImageBitmap()
                        }
                        is ImageResult.Error -> {
                            println("rememberImageFromUrl: Failed for $finalUrl with error: ${result.error}")
                            null
                        }
                        else -> {
                            null
                        }
                    }
                } catch (e: Exception) {
                    println("rememberImageFromUrl: Error loading image from url: $url, error: ${e.message}")
                    null
                }
            }
        } else {
            null
        }
    }

    return imageBitmap
}
