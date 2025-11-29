package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Puppet
import org.menagerie.puppet_master.Troupe

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun PuppetControls(
    troupe: Troupe?,
    activePuppet: Puppet?,
    onPuppetSelected: (String) -> Unit,
    onPuppetCreated: (String) -> Unit,
    onHover: (Boolean) -> Unit
) {
    var newPuppetName by remember { mutableStateOf("") }
    var puppetExpanded by remember { mutableStateOf(false) }
    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ExposedDropdownMenuBox(expanded = puppetExpanded, onExpandedChange = { puppetExpanded = !puppetExpanded }, modifier = Modifier.weight(1f)) {
            TextField(
                modifier = Modifier.menuAnchor().fillMaxWidth()
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["puppetDropdown"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["puppetDropdown"] = false },
                value = activePuppet?.name ?: "", onValueChange = {},
                label = { Text("Active Puppet") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = puppetExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded = puppetExpanded, onDismissRequest = { puppetExpanded = false }) {
                troupe?.puppets?.forEachIndexed { index, puppet ->
                    DropdownMenuItem(
                        text = { Text(puppet.name) },
                        onClick = { onPuppetSelected(puppet.name); puppetExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["puppetItem$index"] = true }
                            .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["puppetItem$index"] = false }
                    )
                }
            }
        }
        TextField(
            value = newPuppetName, 
            onValueChange = {newPuppetName = it}, 
            placeholder = {Text("New Puppet Name")}, 
            modifier = Modifier.weight(1f).padding(start = 8.dp)
                .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["newPuppetName"] = true }
                .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["newPuppetName"] = false },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (newPuppetName.isNotBlank()) { 
                        onPuppetCreated(newPuppetName)
                        newPuppetName = "" 
                    }
                }
            )
        )
        Button(
            onClick = { if (newPuppetName.isNotBlank()) { onPuppetCreated(newPuppetName); newPuppetName = "" } },
            modifier = Modifier.padding(start = 8.dp)
                .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["createPuppet"] = true }
                .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["createPuppet"] = false },
        ){
            Text("Create")
        }
    }
}
