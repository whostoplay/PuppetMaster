package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

/**
 * Returns whether the current platform is desktop.
 * This is the JVM implementation, which always returns true.
 */
@Composable
actual fun isDesktop(): Boolean = true