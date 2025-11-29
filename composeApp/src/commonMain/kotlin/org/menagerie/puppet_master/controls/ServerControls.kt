package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.OperatingMode

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ServerControls(
    operatingMode: OperatingMode,
    isPublishing: Boolean,
    isListening: Boolean,
    onTogglePublishing: (Boolean) -> Unit,
    onToggleListening: () -> Unit,
    onHover: (Boolean) -> Unit
) {
    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Publishing",
                modifier = Modifier
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["publishingText"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["publishingText"] = false }
            )
            Switch(
                checked = isPublishing,
                onCheckedChange = onTogglePublishing,
                enabled = operatingMode == OperatingMode.ONLINE,
                modifier = Modifier
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["publishingSwitch"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["publishingSwitch"] = false }
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "Listening",
                modifier = Modifier
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["listeningText"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["listeningText"] = false }
            )
            Switch(
                checked = isListening,
                onCheckedChange = { onToggleListening() },
                modifier = Modifier
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["listeningSwitch"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["listeningSwitch"] = false }
            )
        }
    }
}
