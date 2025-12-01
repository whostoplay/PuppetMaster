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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.ImageFilePicker
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.toImageBitmap

/**
 * A composable that provides a UI for creating new puppet states.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param viewModel The view model that this composable will interact with.
 * @param selectedImage The currently selected main image.
 * @param selectedImageName The name of the currently selected main image.
 * @param selectedBlinkImage The currently selected blink image.
 * @param selectedBlinkImageName The name of the currently selected blink image.
 * @param newStateName The name of the new state.
 * @param onStateChange A callback that is invoked when any of the state creation parameters change.
 * @param onHover A callback that is invoked when the user hovers over the composable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun StateCreation(
    modifier: Modifier,
    viewModel: MainViewModel,
    selectedImage: ByteArray?,
    selectedImageName: String,
    selectedBlinkImage: ByteArray?,
    selectedBlinkImageName: String,
    newStateName: String,
    onStateChange: (ByteArray?, String, ByteArray?, String, String) -> Unit,
    onHover: (Boolean) -> Unit
) {
    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
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
                Button(
                    onClick = { onStateChange(selectedBlinkImage, selectedBlinkImageName, selectedImage, selectedImageName, newStateName) },
                    modifier = Modifier
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    when (awaitPointerEvent().type) {
                                        PointerEventType.Enter -> isHoveringOnItems["swap"] = true
                                        PointerEventType.Exit -> isHoveringOnItems["swap"] = false
                                    }
                                }
                            }
                        }
                ) {
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

        Box(
            modifier = Modifier
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> isHoveringOnItems["picker"] = true
                                PointerEventType.Exit -> isHoveringOnItems["picker"] = false
                            }
                        }
                    }
                }
        ) {
            ImageFilePicker("Select Image(s)") { images ->
                val mainImage = images.getOrNull(0)
                val blinkImage = images.getOrNull(1)
                onStateChange(mainImage?.first, mainImage?.second ?: "", blinkImage?.first, blinkImage?.second ?: "", newStateName)
            }
        }

        var expanded by remember { mutableStateOf(false) }
        val predefinedStates = remember { listOf("idle", "talking", "listening", "shocked", "crying") }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier.menuAnchor().fillMaxWidth()
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                when (awaitPointerEvent().type) {
                                    PointerEventType.Enter -> isHoveringOnItems["nameField"] = true
                                    PointerEventType.Exit -> isHoveringOnItems["nameField"] = false
                                }
                            }
                        }
                    },
                value = newStateName, onValueChange = { onStateChange(selectedImage, selectedImageName, selectedBlinkImage, selectedBlinkImageName, it) },
                label = { Text("State Name") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                predefinedStates.forEachIndexed { index, selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = { onStateChange(selectedImage, selectedImageName, selectedBlinkImage, selectedBlinkImageName, selectionOption); expanded = false },
                        modifier = Modifier
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        when (awaitPointerEvent().type) {
                                            PointerEventType.Enter -> isHoveringOnItems["menuItem$index"] = true
                                            PointerEventType.Exit -> isHoveringOnItems["menuItem$index"] = false
                                        }
                                    }
                                }
                            }
                    )
                }
            }
        }

        val isSaveEnabled = selectedImage != null && newStateName.isNotBlank()

        if (!isSaveEnabled) {
            val missingParts = mutableListOf<String>()
            if (selectedImage == null) {
                missingParts.add("an image")
            }
            if (newStateName.isBlank()) {
                missingParts.add("a state name")
            }
            Text(
                "Please select ${missingParts.joinToString(" and ")}.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        Button(
            onClick = {
                viewModel.createNewState()
            },
            enabled = isSaveEnabled,
            modifier = Modifier.padding(top = 8.dp)
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            when (awaitPointerEvent().type) {
                                PointerEventType.Enter -> isHoveringOnItems["save"] = true
                                PointerEventType.Exit -> isHoveringOnItems["save"] = false
                            }
                        }
                    }
                }
        ) { Text("Save Local State") }
    }
}
