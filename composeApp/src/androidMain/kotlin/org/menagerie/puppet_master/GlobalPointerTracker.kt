package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

/**
 * Remembers the global pointer position, or null if the pointer is not in the window.
 *
 * @param window The window to track the pointer in. On Android, this should be the result of `LocalView.current.context as Activity).window`.
 */
@Composable
actual fun rememberGlobalPointerPosition(window: Any?): Offset? {
    return null
}
