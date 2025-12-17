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
import java.awt.Window

@Composable
actual fun rememberGlobalPointerPosition(window: Any?): Offset? {
    var pointer by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(window) {
        if (window !is Window) return@LaunchedEffect
        while (true) {
            val pointerInfo = MouseInfo.getPointerInfo()
            if (pointerInfo != null) {
                val location = pointerInfo.location
                val windowLocation = window.locationOnScreen
                pointer = Offset(
                    location.x.toFloat() - windowLocation.x,
                    location.y.toFloat() - windowLocation.y
                )
            }
            delay(100) // Poll every 100ms
        }
    }

    return pointer
}
