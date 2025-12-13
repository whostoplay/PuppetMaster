package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/**
 * A composable that remembers and loads an image from a network URL.
 * Returns null if the image is loading or fails to load.
 */
@Composable
expect fun rememberImageFromUrl(url: String, forceReloadKey: Any?): ImageBitmap?
