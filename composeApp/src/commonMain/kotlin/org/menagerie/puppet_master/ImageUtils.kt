package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

expect fun decodeToImageBitmap(byteArray: ByteArray): ImageBitmap
expect fun readFileAsByteArray(directory: String, filename: String): ByteArray?

@Composable
expect fun rememberColorMapBitmap(width: Int, height: Int, value: Float): ImageBitmap
