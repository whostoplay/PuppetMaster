package org.menagerie.puppet_master

import androidx.compose.runtime.Composable

expect fun hasAudioPermission(context: Any): Boolean

@Composable
expect fun RequestAudioPermission(onResult: (granted: Boolean) -> Unit)
