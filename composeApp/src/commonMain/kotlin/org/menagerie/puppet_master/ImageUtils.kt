package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeToImageBitmap(byteArray: ByteArray): ImageBitmap

@Composable
expect fun rememberColorMapBitmap(width: Int, height: Int): ImageBitmap
