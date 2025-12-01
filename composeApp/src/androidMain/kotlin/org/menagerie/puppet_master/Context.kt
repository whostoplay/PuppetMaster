package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Returns the current Android context.
 */
@Composable
actual fun getContext(): Any {
    return LocalContext.current
}