package org.menagerie.puppet_master.controls

import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetTroupe
import org.menagerie.puppet_master.localisation.Strings

/**
 * A composable that provides UI controls for managing puppets and troupes.
 *
 * This includes selecting an active puppet, creating new puppets, loading, renaming, and creating new troupes,
 * as well as importing and exporting individual puppets.
 *
 * @param troupe The current [PuppetTroupe], or null if no troupe is loaded.
 * @param activePuppet The currently active [PuppetCharacter], or null if none is selected.
 * @param onPuppetSelected Callback invoked with the name of the puppet when a puppet is selected.
 * @param onPuppetCreated Callback invoked when a new puppet is created. It provides the new puppet's name and an optional new troupe name.
 * @param onImportPuppet Callback to trigger the import of a puppet.
 * @param onExportPuppet Callback to trigger the export of the active puppet, providing its name.
 * @param onRenameTroupe Callback to rename the current troupe, providing the new name.
 * @param onLoadTroupe Callback to trigger loading a troupe.
 * @param onNewTroupeCreated Callback to trigger the creation of a new, empty troupe.
 * @param onActiveChange Callback to report whether the user is currently interacting with the controls (e.g., hovering or focusing).
 * @param onFocusChange Callback to report whether any input fields within the controls have focus.
 * @param rootFocusRequester A [FocusRequester] to return focus to the main application area after an action is completed.
 */
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
    onNewTroupeCreated: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester
) {
    var newPuppetName by remember { mutableStateOf("") }
    var puppetExpanded by remember { mutableStateOf(false) }
    var showNameTroupeDialog by remember { mutableStateOf(false) }
    var showRenameTroupeDialog by remember { mutableStateOf(false) }

    val textFieldInteractionSource = remember { MutableInteractionSource() }
    val menuInteractionSource = remember { MutableInteractionSource() }
    val isTextFieldHovered by textFieldInteractionSource.collectIsHoveredAsState()
    val isMenuHovered by menuInteractionSource.collectIsHoveredAsState()
    val isHovered = isTextFieldHovered || isMenuHovered

    var isNewPuppetNameFocused by remember { mutableStateOf(false) }

    val isPanelActive = isHovered || isNewPuppetNameFocused || puppetExpanded

    LaunchedEffect(isPanelActive) {
        onActiveChange(isPanelActive)
    }
    LaunchedEffect(isNewPuppetNameFocused) {
        onFocusChange(isNewPuppetNameFocused)
    }

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
        ExposedDropdownMenuBox(
            expanded = puppetExpanded,
            onExpandedChange = { puppetExpanded = !puppetExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            TextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .hoverable(textFieldInteractionSource),
                value = activePuppet?.name ?: "", onValueChange = {},
                label = { Text(Strings.getString(Strings.Keys.ACTIVE_PUPPET_LABEL)) },
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = puppetExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(
                expanded = puppetExpanded,
                onDismissRequest = { puppetExpanded = false },
                modifier = Modifier.hoverable(menuInteractionSource)
            ) {
                troupe?.puppets?.forEach { puppet ->
                    DropdownMenuItem(
                        text = { Text(puppet.name) },
                        onClick = { onPuppetSelected(puppet.name); puppetExpanded = false },
                        contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onLoadTroupe) {
                Text(Strings.getString(Strings.Keys.LOAD_TROUPE_BUTTON))
            }
            Button(onClick = { showRenameTroupeDialog = true }, enabled = troupe != null) {
                Text(Strings.getString(Strings.Keys.RENAME_TROUPE_BUTTON))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImportPuppet) {
                Text(Strings.getString(Strings.Keys.IMPORT_PUPPET_BUTTON))
            }
            Button(
                onClick = { activePuppet?.let { onExportPuppet(it.name) } },
                enabled = activePuppet != null
            ) {
                Text(Strings.getString(Strings.Keys.EXPORT_ACTIVE_PUPPET_BUTTON))
            }

        }
        HorizontalDivider()
        TextField(
            value = newPuppetName,
            onValueChange = { newPuppetName = it },
            placeholder = { Text(Strings.getString(Strings.Keys.NEW_PUPPET_NAME_PLACEHOLDER)) },
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    isNewPuppetNameFocused = focusState.isFocused
                    if (!focusState.isFocused) {
                        rootFocusRequester.requestFocus()
                    }
                },
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
                    rootFocusRequester.requestFocus()
                }
            )
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            ) {
                Text(Strings.getString(Strings.Keys.CREATE_BUTTON))
            }
            Button(onClick = onNewTroupeCreated) {
                Text(Strings.getString(Strings.Keys.CREATE_NEW_TROUPE_BUTTON))
            }
        }
    }
}
