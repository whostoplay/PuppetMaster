package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

/**
 * Returns the current JVM context.
 * This is a placeholder implementation for the desktop target, as a context object is not needed.
 */
@Composable
actual fun getContext(): Any {
    return Any()
}