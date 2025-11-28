package org.menagerie.puppet_master

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun getContext(): Any {
    return LocalContext.current
}