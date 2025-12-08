package org.menagerie.puppet_master

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset

@Composable
expect fun rememberGlobalPointerPosition(): Offset?
