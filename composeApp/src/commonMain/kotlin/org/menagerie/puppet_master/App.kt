package org.menagerie.puppet_master

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val context = getContext()
    val viewModel = remember { MainViewModel(context) }
    val troupe by viewModel.troupe.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val displayedImageName by viewModel.displayedImageName.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()

    var selectedImage by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageName by remember { mutableStateOf("") }
    var selectedBlinkImage by remember { mutableStateOf<ByteArray?>(null) }
    var selectedBlinkImageName by remember { mutableStateOf("") }
    var newStateName by remember { mutableStateOf("") }
    var newPuppetName by remember { mutableStateOf("") }

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
                    "http://127.0.0.1:$SERVER_PORT/uploads/${displayedImageName ?: ""}"
                } else {
                    if (displayedImageName?.isNotBlank() == true) "file://${viewModel.uploadsDir}/$displayedImageName" else ""
                }
                val image = rememberImageFromUrl(imageUrl)

                if (image != null) {
                    Image(bitmap = image, contentDescription = "Live Preview")
                } else {
                    Text("No Active Image")
                }
            }

            // --- Puppet Selection ---
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                var puppetExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = puppetExpanded, onExpandedChange = { puppetExpanded = !puppetExpanded }, modifier = Modifier.weight(1f)) {
                    TextField(
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        value = activePuppet?.name ?: "", onValueChange = {},
                        label = { Text("Active Puppet") },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = puppetExpanded) },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                    )
                    ExposedDropdownMenu(expanded = puppetExpanded, onDismissRequest = { puppetExpanded = false }) {
                        troupe?.puppets?.forEach { puppet ->
                            DropdownMenuItem(text = { Text(puppet.name) }, onClick = { viewModel.setActivePuppet(puppet.name); puppetExpanded = false }, contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding)
                        }
                    }
                }
                TextField(
                    value = newPuppetName, 
                    onValueChange = {newPuppetName = it}, 
                    placeholder = {Text("New Puppet Name")}, 
                    modifier = Modifier.weight(1f).padding(start = 8.dp),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (newPuppetName.isNotBlank()) { 
                                viewModel.createNewPuppet(newPuppetName)
                                newPuppetName = "" 
                            }
                        }
                    )
                )
                Button(onClick = { if (newPuppetName.isNotBlank()) { viewModel.createNewPuppet(newPuppetName); newPuppetName = "" } }, modifier = Modifier.padding(start = 8.dp)) { Text("Create") }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
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
            Row(modifier = Modifier.fillMaxWidth().padding(8.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { viewModel.publishTroupe() }, enabled = operatingMode == OperatingMode.ONLINE) { Text("Upload Troupe") }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Publishing")
                    Switch(
                        checked = isPublishing,
                        onCheckedChange = { viewModel.togglePublishing() },
                        enabled = operatingMode == OperatingMode.ONLINE
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Listening")
                    Switch(
                        checked = isListening,
                        onCheckedChange = { viewModel.toggleListening() }
                    )
                }
            }
            HorizontalDivider()

            // --- State Creation & Listing ---
            if (activePuppet != null) {
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.weight(1f).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Create New State", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            selectedImage?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Main Image")
                                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Image", modifier = Modifier.size(100.dp))
                                }
                            }

                            if (selectedImage != null && selectedBlinkImage != null) {
                                Button(onClick = {
                                    val tempImg = selectedImage
                                    selectedImage = selectedBlinkImage
                                    selectedBlinkImage = tempImg

                                    val tempName = selectedImageName
                                    selectedImageName = selectedBlinkImageName
                                    selectedBlinkImageName = tempName
                                }) {
                                    Text("<->")
                                }
                            }

                            selectedBlinkImage?.let {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Blink Image")
                                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Blink Image", modifier = Modifier.size(100.dp))
                                }
                            }
                        }

                        ImageFilePicker("Select Image(s)") { images ->
                            if (images.isNotEmpty()) {
                                selectedImage = images[0].first
                                selectedImageName = images[0].second
                            }
                            if (images.size > 1) {
                                selectedBlinkImage = images[1].first
                                selectedBlinkImageName = images[1].second
                            } else {
                                selectedBlinkImage = null
                                selectedBlinkImageName = ""
                            }
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
                                viewModel.createNewState(newStateName, selectedImage!!, selectedImageName, selectedBlinkImage, selectedBlinkImageName)
                                newStateName = ""
                                selectedImage = null
                                selectedImageName = ""
                                selectedBlinkImage = null
                                selectedBlinkImageName = ""
                            }
                        }, modifier = Modifier.padding(top = 8.dp)) { Text("Save Local State") }
                    }
                    
                    VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))

                    LazyColumn(modifier = Modifier.weight(1f).padding(8.dp)) {
                        item { Text("Puppet States", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 8.dp)) }
                        activePuppet?.states?.let {
                            items(it) { state ->
                                val blinkText = if (state.blinkImageName != null) " (has blink)" else ""
                                Text("State: ${state.name} -> ${state.imageName}$blinkText", modifier = Modifier.padding(4.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
expect fun ByteArray.toImageBitmap(): ImageBitmap