package org.menagerie.puppet_master.states

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetStateInfo

@Composable
fun StateListing(
    modifier: Modifier,
    activePuppet: PuppetCharacter?,
    selectedState: PuppetStateInfo?,
    onStateSelected: (PuppetStateInfo) -> Unit
) {
    LazyColumn(
        modifier = modifier.padding(8.dp)
    ) {
        item { Text("Puppet States", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
        activePuppet?.states?.let {
            items(it) { state ->
                val blinkText = if (state.blinkImageName != null) " (has blink)" else ""
                val backgroundColor = if (state == selectedState) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                Text(
                    text = "State: ${state.name} -> ${state.imageName}$blinkText",
                    modifier = Modifier.padding(4.dp).background(backgroundColor).clickable { onStateSelected(state) }
                )
            }
        }
    }
}
