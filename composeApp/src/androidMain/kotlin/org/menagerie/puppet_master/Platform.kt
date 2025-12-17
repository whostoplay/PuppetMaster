package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

/**
 * Returns whether the current platform is desktop.
 * This is the Android implementation, which always returns false.
 */
@Composable
actual fun isDesktop(): Boolean = false