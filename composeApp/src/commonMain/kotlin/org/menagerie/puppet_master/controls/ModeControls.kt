package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.OperatingMode

/**
 * A composable that provides a switch to toggle between online and offline operating modes.
 *
 * @param operatingMode The current operating mode.
 * @param onModeChanged A callback that is invoked when the operating mode is changed.
 */
@Composable
fun ModeControls(
    operatingMode: OperatingMode,
    onModeChanged: (OperatingMode) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "Offline")
        Switch(
            checked = operatingMode == OperatingMode.ONLINE,
            onCheckedChange = { isOnline -> onModeChanged(if (isOnline) OperatingMode.ONLINE else OperatingMode.OFFLINE) },
            modifier = Modifier.padding(horizontal = 8.dp)
        )
        Text(text = "Online")
    }
}
