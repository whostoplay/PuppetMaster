package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
 * A composable that provides controls for interacting with the server.
 *
 * @param operatingMode The current operating mode.
 * @param isPublishing Whether the client is currently publishing its state to the server.
 * @param isListening Whether the client is currently listening for audio input.
 * @param onTogglePublishing A callback that is invoked when the user toggles the publishing switch.
 * @param onToggleListening A callback that is invoked when the user toggles the listening switch.
 */
@Composable
fun ServerControls(
    operatingMode: OperatingMode,
    isPublishing: Boolean,
    isListening: Boolean,
    onTogglePublishing: (Boolean) -> Unit,
    onToggleListening: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Publishing")
            Switch(
                checked = isPublishing,
                onCheckedChange = onTogglePublishing,
                enabled = operatingMode == OperatingMode.ONLINE
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Listening")
            Switch(
                checked = isListening,
                onCheckedChange = { onToggleListening() }
            )
        }
    }
}
