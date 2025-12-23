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
import org.menagerie.puppet_master.ControlMode
import org.menagerie.puppet_master.OperatingMode
import org.menagerie.puppet_master.localisation.Strings

/**
 * A composable that provides switches to toggle between operating modes and control modes.
 *
 * @param operatingMode The current [OperatingMode].
 * @param onModeChanged A callback that is invoked with the new [OperatingMode] when the switch is toggled.
 * @param controlMode The current [ControlMode].
 * @param onControlModeChanged A callback that is invoked with the new [ControlMode] when the switch is toggled.
 */
@Composable
fun ModeControls(
    operatingMode: OperatingMode,
    onModeChanged: (OperatingMode) -> Unit,
    controlMode: ControlMode,
    onControlModeChanged: (ControlMode) -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = Strings.getString(Strings.Keys.OFFLINE))
            Switch(
                checked = operatingMode == OperatingMode.ONLINE,
                onCheckedChange = { isOnline -> onModeChanged(if (isOnline) OperatingMode.ONLINE else OperatingMode.OFFLINE) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(text = Strings.getString(Strings.Keys.ONLINE))
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = Strings.getString(Strings.Keys.DIRECT))
            Switch(
                checked = controlMode == ControlMode.STATE_MACHINE,
                onCheckedChange = { isStateMachine -> onControlModeChanged(if (isStateMachine) ControlMode.STATE_MACHINE else ControlMode.DIRECT) },
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Text(text = Strings.getString(Strings.Keys.STATE_MACHINE))
        }
    }
}
