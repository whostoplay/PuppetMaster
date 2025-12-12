package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetTroupe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PuppetControls(
    troupe: PuppetTroupe?,
    activePuppet: PuppetCharacter?,
    onPuppetSelected: (String) -> Unit,
    onPuppetCreated: (newPuppetName: String, newTroupeName: String?) -> Unit,
    onImportPuppet: () -> Unit,
    onExportPuppet: (puppetName: String) -> Unit,
    onRenameTroupe: (newName: String) -> Unit,
    onLoadTroupe: () -> Unit,
    onNewTroupeCreated: () -> Unit
) {
    var newPuppetName by remember { mutableStateOf("") }
    var puppetExpanded by remember { mutableStateOf(false) }
    var showNameTroupeDialog by remember { mutableStateOf(false) }
    var showRenameTroupeDialog by remember { mutableStateOf(false) }

    if (showNameTroupeDialog) {
        NameTroupeDialog(
            onConfirm = {
                onPuppetCreated(newPuppetName, it)
                newPuppetName = ""
                showNameTroupeDialog = false
            },
            onDismiss = { showNameTroupeDialog = false }
        )
    }

    if (showRenameTroupeDialog) {
        RenameTroupeDialog(
            currentName = troupe?.name ?: "",
            onConfirm = {
                onRenameTroupe(it)
                showRenameTroupeDialog = false
            },
            onDismiss = { showRenameTroupeDialog = false }
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ExposedDropdownMenuBox(expanded = puppetExpanded, onExpandedChange = { puppetExpanded = !puppetExpanded }, modifier = Modifier.fillMaxWidth()) {
            TextField(
                modifier = Modifier.fillMaxWidth(),
                value = activePuppet?.name ?: "", onValueChange = {},
                label = { Text("Active Puppet") },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = puppetExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded = puppetExpanded, onDismissRequest = { puppetExpanded = false }) {
                troupe?.puppets?.forEach { puppet ->
                    DropdownMenuItem(
                        text = { Text(puppet.name) },
                        onClick = { onPuppetSelected(puppet.name); puppetExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
        TextField(
            value = newPuppetName,
            onValueChange = {newPuppetName = it},
            placeholder = {Text("New Puppet Name")},
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (newPuppetName.isNotBlank()) {
                        if (troupe?.puppets?.isEmpty() != false) {
                            showNameTroupeDialog = true
                        } else {
                            onPuppetCreated(newPuppetName, null)
                            newPuppetName = ""
                        }
                    }
                }
            )
        )
        Button(
            onClick = { 
                if (newPuppetName.isNotBlank()) { 
                    if (troupe?.puppets?.isEmpty() != false) {
                        showNameTroupeDialog = true
                    } else {
                        onPuppetCreated(newPuppetName, null)
                        newPuppetName = ""
                    } 
                }
             },
        ){
            Text("Create")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onLoadTroupe) {
                Text("Load Troupe")
            }
            Button(onClick = { showRenameTroupeDialog = true }, enabled = troupe != null) {
                Text("Rename Troupe")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImportPuppet) {
                Text("Import Puppet")
            }
            Button(onClick = { activePuppet?.let { onExportPuppet(it.name) } }, enabled = activePuppet != null) {
                Text("Export Active Puppet")
            }

        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onNewTroupeCreated) {
                Text("Create New Troupe")
            }
        }
    }
}
