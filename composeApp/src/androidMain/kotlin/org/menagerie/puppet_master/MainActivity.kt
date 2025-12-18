package org.menagerie.puppet_master

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.tooling.preview.Preview
import com.seiko.imageloader.ImageLoader
import com.seiko.imageloader.LocalImageLoader
import com.seiko.imageloader.cache.disk.DiskCache
import com.seiko.imageloader.cache.memory.MemoryCache
import com.seiko.imageloader.cache.memory.maxSizePercent
import com.seiko.imageloader.component.setupDefaultComponents
import com.seiko.imageloader.intercept.DiskCacheInterceptor
import com.seiko.imageloader.intercept.MemoryCacheInterceptor
import okio.Path.Companion.toOkioPath

/**
 * The main activity for the Android application.
 * This activity sets up the Compose content and provides an [ImageLoader] to the `App` composable.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalImageLoader provides generateImageLoader(this)) {
                App()
            }
        }
    }
}

/**
 * Generates an [ImageLoader] with a memory and disk cache.
 *
 * @param context The application context.
 * @return An [ImageLoader] instance.
 */
private fun generateImageLoader(context: Context): ImageLoader {
    return ImageLoader { 
        components {
            setupDefaultComponents(context)
        }
        interceptor {
            addInterceptor(MemoryCacheInterceptor {
                MemoryCache {
                    maxSizePercent(context, Constants.Caching.MEMORY_CACHE_MAX_SIZE_PERCENT)
                }
            })
            addInterceptor(DiskCacheInterceptor {
                DiskCache {
                    directory(context.cacheDir.resolve(Constants.Caching.IMAGE_CACHE_DIRECTORY).toOkioPath())
                    maxSizeBytes(Constants.Caching.DISK_CACHE_MAX_SIZE_BYTES)
                }
            })
        }
    }
}