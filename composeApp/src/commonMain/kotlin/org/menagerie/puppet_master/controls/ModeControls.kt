package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.OperatingMode

/**
 * A composable that provides a switch to toggle between online and offline operating modes.
 *
 * @param operatingMode The current operating mode.
 * @param onModeChanged A callback that is invoked when the operating mode is changed.
 * @param onHover A callback that is invoked when the user hovers over the controls.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ModeControls(
    operatingMode: OperatingMode,
    onModeChanged: (OperatingMode) -> Unit,
    onHover: (Boolean) -> Unit
) {
    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Offline",
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> isHoveringOnItems["offlineText"] = true
                                PointerEventType.Exit -> isHoveringOnItems["offlineText"] = false
                            }
                        }
                    }
                }
        )
        Switch(
            checked = operatingMode == OperatingMode.ONLINE,
            onCheckedChange = { isOnline -> onModeChanged(if (isOnline) OperatingMode.ONLINE else OperatingMode.OFFLINE) },
            modifier = Modifier.padding(horizontal = 8.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> isHoveringOnItems["onlineSwitch"] = true
                                PointerEventType.Exit -> isHoveringOnItems["onlineSwitch"] = false
                            }
                        }
                    }
                }
        )
        Text(
            text = "Online",
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> isHoveringOnItems["onlineText"] = true
                                PointerEventType.Exit -> isHoveringOnItems["onlineText"] = false
                            }
                        }
                    }
                }
        )
    }
}
