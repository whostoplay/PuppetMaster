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

private fun generateImageLoader(context: Context): ImageLoader {
    return ImageLoader { 
        components {
            setupDefaultComponents(context)
        }
        interceptor {
            addInterceptor(MemoryCacheInterceptor {
                MemoryCache {
                    maxSizePercent(context, 0.25)
                }
            })
            addInterceptor(DiskCacheInterceptor {
                DiskCache {
                    directory(context.cacheDir.resolve("image_cache").toOkioPath())
                    maxSizeBytes(512L * 1024 * 1024)
                }
            })
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}