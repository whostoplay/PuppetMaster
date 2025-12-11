package org.menagerie.puppet_master.states

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.menagerie.puppet_master.ImageFilePicker
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.toImageBitmap

/**
 * A composable that provides a UI for creating new puppet states.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param viewModel The view model that this composable will interact with.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateCreation(
    modifier: Modifier,
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Create New State", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.selectedImage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Main Image")
                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Image", modifier = Modifier.size(100.dp))
                }
            }

            if (uiState.selectedImage != null && uiState.selectedBlinkImage != null) {
                Button(
                    onClick = { viewModel.onStateCreationChange(uiState.selectedBlinkImage, uiState.selectedBlinkImageName, uiState.selectedImage, uiState.selectedImageName, uiState.newStateName) }
                ) {
                    Text("<->")
                }
            }

            uiState.selectedBlinkImage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Blink Image")
                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Blink Image", modifier = Modifier.size(100.dp))
                }
            }
        }

        Box {
            ImageFilePicker("Select Image(s)") { images ->
                val mainImage = images.getOrNull(0)
                val blinkImage = images.getOrNull(1)
                viewModel.onStateCreationChange(mainImage?.first, mainImage?.second ?: "", blinkImage?.first, blinkImage?.second ?: "", uiState.newStateName)
            }
        }

        var expanded by remember { mutableStateOf(false) }
        val predefinedStates = remember { listOf("idle", "talking", "listening", "shocked", "crying") }
        val customStates = remember(activePuppet) {
            activePuppet?.states?.map { it.name }?.filter { it !in predefinedStates } ?: emptyList()
        }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                value = uiState.newStateName, onValueChange = { viewModel.onStateCreationChange(uiState.selectedImage, uiState.selectedImageName, uiState.selectedBlinkImage, uiState.selectedBlinkImageName, it) },
                label = { Text("State Name") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                (predefinedStates + customStates).distinct().forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = { 
                            coroutineScope.launch {
                                val state = activePuppet?.states?.find { it.name == selectionOption }
                                val mainImage = state?.imageName?.let { viewModel.getImageData(it) }
                                val blinkImage = state?.blinkImageName?.let { viewModel.getImageData(it) }
                                viewModel.onStateCreationChange(mainImage, state?.imageName ?: "", blinkImage, state?.blinkImageName ?: "", selectionOption)
                            }
                            expanded = false 
                        }
                    )
                }
            }
        }

        val isSaveEnabled = uiState.selectedImage != null && uiState.newStateName.isNotBlank()

        if (!isSaveEnabled) {
            val missingParts = mutableListOf<String>()
            if (uiState.selectedImage == null) {
                missingParts.add("an image")
            }
            if (uiState.newStateName.isBlank()) {
                missingParts.add("a state name")
            }
            Text(
                "Please select ${missingParts.joinToString(" and ")} to create or update a state.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                viewModel.onSaveOrUpdateStateClicked()
            },
            enabled = isSaveEnabled,
            modifier = Modifier.padding(top = 8.dp)
        ) { Text("Save/Update State") }
    }
}