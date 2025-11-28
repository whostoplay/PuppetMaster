package org.menagerie.puppet_master

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val context = getContext()
    val viewModel = remember { MainViewModel(context) }
    val localAvatarConfig by viewModel.localAvatarConfig.collectAsState()
    val activeState by viewModel.activeState.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()

    var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    var newStateName by remember { mutableStateOf("") }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // --- Live Preview ---
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp).padding(8.dp).border(1.dp, Color.Gray),
                contentAlignment = Alignment.Center
            ) {
                val imageUrl = if (operatingMode == OperatingMode.ONLINE) {
                    "http://127.0.0.1:$SERVER_PORT/uploads/${activeState?.imageName ?: ""}"
                } else {
                    val imageName = activeState?.imageName ?: ""
                    if (imageName.isNotBlank()) "file://${viewModel.uploadsDir}/$imageName" else ""
                }
                val image = rememberImageFromUrl(imageUrl)

                if (image != null) {
                    Image(bitmap = image, contentDescription = "Live Preview")
                } else {
                    Text("No Active Image")
                }
            }
            
            // --- Mode Controls ---
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), horizontalArrangement = Arrangement.Center) {
                val onlineColors = if(operatingMode == OperatingMode.ONLINE) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors()
                val offlineColors = if(operatingMode == OperatingMode.OFFLINE) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.outlinedButtonColors()

                Button(onClick = { viewModel.setOperatingMode(OperatingMode.ONLINE) }, colors = onlineColors) { Text("Online") }
                Spacer(modifier = Modifier.width(8.dp))
                Button(onClick = { viewModel.setOperatingMode(OperatingMode.OFFLINE) }, colors = offlineColors) { Text("Offline") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // --- Server & Audio Controls ---
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                Button(onClick = { viewModel.publishConfiguration() }, enabled = operatingMode == OperatingMode.ONLINE) { Text("Publish to Server") }
                Button(onClick = { viewModel.startListening() }) { Text("Start Listening") }
                Button(onClick = { viewModel.stopListening() }) { Text("Stop Listening") }
            }
            HorizontalDivider()

            // --- State Creation & Listing ---
            Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.weight(1f).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Create New State", style = MaterialTheme.typography.titleMedium)
                    selectedImage?.let {
                        Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Image", modifier = Modifier.size(100.dp).padding(vertical = 8.dp))
                    }

                    ImageFilePicker { bytes, fileName ->
                        selectedImage = bytes
                        selectedImageName = fileName
                    }

                    var expanded by remember { mutableStateOf(false) }
                    val predefinedStates = remember { listOf("idle", "talking", "listening", "shocked", "crying") }

                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                        TextField(
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            value = newStateName, onValueChange = { newStateName = it },
                            label = { Text("State Name") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            predefinedStates.forEach { selectionOption ->
                                DropdownMenuItem(text = { Text(selectionOption) }, onClick = { newStateName = selectionOption; expanded = false }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
                            }
                        }
                    }

                    Button(onClick = {
                        if (selectedImage != null && newStateName.isNotBlank()) {
                            viewModel.createNewState(newStateName, selectedImage!!, selectedImageName)
                            newStateName = ""
                            selectedImage = null
                            selectedImageName = ""
                        }
                    }, modifier = Modifier.padding(top = 8.dp)) { Text("Save Local State") }
                }
                
                VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

                LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                    item { Text("Local Avatar States", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                    localAvatarConfig?.states?.let {
                        items(it) { state ->
                            Text("State: ${state.name} -> ${state.imageName}", modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun ByteArray.toImageBitmap(): ImageBitmap