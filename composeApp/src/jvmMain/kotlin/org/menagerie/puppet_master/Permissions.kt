package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

actual fun hasAudioPermission(context: Any): Boolean = true

@Composable
actual fun RequestAudioPermission(onResult: (granted: Boolean) -> Unit) {}
