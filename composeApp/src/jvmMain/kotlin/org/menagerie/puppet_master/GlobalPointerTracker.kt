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

@Composable
actual fun rememberGlobalPointerPosition(): Offset? {
    var pointer by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        while (true) {
            val location = MouseInfo.getPointerInfo()?.location
            if (location != null) {
                pointer = Offset(location.x.toFloat(), location.y.toFloat())
            }
            delay(100) // Poll every 100ms
        }
    }

    return pointer
}
