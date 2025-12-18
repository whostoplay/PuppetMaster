package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import java.awt.MouseInfo
import java.awt.Window

/**
 * A Composable function that remembers the global pointer position on the screen.
 *
 * This function is the JVM-specific implementation of `rememberGlobalPointerPosition`.
 * It uses AWT's `MouseInfo` to get the pointer's location and updates it periodically.
 * The position is returned as an [Offset] relative to the given [window].
 *
 * @param window The window (expected to be a `java.awt.Window`) to which the pointer position should be relative.
 * If the provided `window` is not a `java.awt.Window`, the function will not track the pointer.
 * @return An [Offset] representing the pointer's position relative to the window, or `null` if the pointer's position cannot be determined.
 */
@Composable
actual fun rememberGlobalPointerPosition(window: Any?): Offset? {
    var pointer by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(window) {
        if (window !is Window) return@LaunchedEffect
        while (true) {
            val pointerInfo = MouseInfo.getPointerInfo()
            if (pointerInfo != null) {
                val location = pointerInfo.location
                val windowLocation = window.locationOnScreen
                pointer = Offset(
                    location.x.toFloat() - windowLocation.x,
                    location.y.toFloat() - windowLocation.y
                )
            }
            delay(100) // Poll every 100ms
        }
    }

    return pointer
}
