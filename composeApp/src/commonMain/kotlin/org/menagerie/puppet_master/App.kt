package org.menagerie.puppet_master

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val viewModel = remember { MainViewModel() }
    val localAvatarStates by viewModel.localAvatarStates.collectAsState()
    
    var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    var newStateName by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- Server Controls ---
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
                Button(onClick = { viewModel.publishConfiguration() }) {
                    Text("Publish to Server")
                }
            }
            Divider()

            // --- State Creation ---
            Column(modifier = Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                selectedImage?.let {
                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Image", modifier = Modifier.size(128.dp))
                }

                ImageFilePicker { bytes, fileName ->
                    selectedImage = bytes
                    selectedImageName = fileName
                }

                // --- Editable Dropdown for State Name ---
                var expanded by remember { mutableStateOf(false) }
                val predefinedStates = remember { listOf("idle", "talking", "listening", "shocked", "crying") }

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                ) {
                    TextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = newStateName,
                        onValueChange = { newStateName = it },
                        label = { Text("New State Name") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        predefinedStates.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption) },
                                onClick = {
                                    newStateName = selectionOption
                                    expanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                            )
                        }
                    }
                }

                Button(onClick = {
                    if (selectedImage != null && newStateName.isNotBlank()) {
                        viewModel.createNewState(newStateName, selectedImage!!, selectedImageName)
                        // Clear inputs after creation
                        newStateName = ""
                        selectedImage = null
                        selectedImageName = ""
                    }
                }) {
                    Text("Create Local State")
                }
            }
            Divider()

            // --- Local State Listing ---
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(localAvatarStates) { state ->
                    Row(modifier = Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "State: ${state.name} -> Image: ${state.imageName}", modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
expect fun ByteArray.toImageBitmap(): ImageBitmap