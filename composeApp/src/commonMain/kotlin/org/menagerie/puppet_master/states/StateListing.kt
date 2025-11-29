package org.menagerie.puppet_master.states

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Puppet

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StateListing(modifier: Modifier, activePuppet: Puppet?, onHover: (Boolean) -> Unit) {
    LazyColumn(
        modifier = modifier.padding(8.dp)
            .onPointerEvent(PointerEventType.Enter) { onHover(true) }
            .onPointerEvent(PointerEventType.Exit) { onHover(false) }
    ) {
        item { Text("Puppet States", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
        activePuppet?.states?.let {
            items(it) { state ->
                val blinkText = if (state.blinkImageName != null) " (has blink)" else ""
                Text("State: ${state.name} -> ${state.imageName}$blinkText", modifier = Modifier.padding(4.dp))
            }
        }
    }
}
