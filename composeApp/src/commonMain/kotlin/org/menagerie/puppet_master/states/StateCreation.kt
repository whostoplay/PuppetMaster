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
import androidx.compose.ui.input.pointer.onPointerEvent
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.ImageFilePicker
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.toImageBitmap

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
    var localSelectedImage by remember { mutableStateOf(selectedImage) }
    var localSelectedImageName by remember { mutableStateOf(selectedImageName) }
    var localSelectedBlinkImage by remember { mutableStateOf(selectedBlinkImage) }
    var localSelectedBlinkImageName by remember { mutableStateOf(selectedBlinkImageName) }
    var localNewStateName by remember { mutableStateOf(newStateName) }

    val isHoveringOnItems = remember { mutableStateMapOf<String, Boolean>() }
    val isHovering = isHoveringOnItems.values.any { it }

    LaunchedEffect(isHovering) {
        onHover(isHovering)
    }

    LaunchedEffect(selectedImage, selectedImageName, selectedBlinkImage, selectedBlinkImageName, newStateName) {
        localSelectedImage = selectedImage
        localSelectedImageName = selectedImageName
        localSelectedBlinkImage = selectedBlinkImage
        localSelectedBlinkImageName = selectedBlinkImageName
        localNewStateName = newStateName
    }

    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Create New State", style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            localSelectedImage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Main Image")
                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Image", modifier = Modifier.size(100.dp))
                }
            }

            if (localSelectedImage != null && localSelectedBlinkImage != null) {
                Button(
                    onClick = {
                        val tempImg = localSelectedImage
                        localSelectedImage = localSelectedBlinkImage
                        localSelectedBlinkImage = tempImg

                        val tempName = localSelectedImageName
                        localSelectedImageName = localSelectedBlinkImageName
                        localSelectedBlinkImageName = tempName
                        onStateChange(localSelectedImage, localSelectedImageName, localSelectedBlinkImage, localSelectedBlinkImageName, localNewStateName)
                    },
                    modifier = Modifier
                        .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["swap"] = true }
                        .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["swap"] = false }
                ) {
                    Text("<->")
                }
            }

            localSelectedBlinkImage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Blink Image")
                    Image(bitmap = it.toImageBitmap(), contentDescription = "Selected Blink Image", modifier = Modifier.size(100.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["picker"] = true }
                .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["picker"] = false }
        ) {
            ImageFilePicker("Select Image(s)") { images ->
                if (images.isNotEmpty()) {
                    localSelectedImage = images[0].first
                    localSelectedImageName = images[0].second
                }
                if (images.size > 1) {
                    localSelectedBlinkImage = images[1].first
                    localSelectedBlinkImageName = images[1].second
                } else {
                    localSelectedBlinkImage = null
                    localSelectedBlinkImageName = ""
                }
                onStateChange(localSelectedImage, localSelectedImageName, localSelectedBlinkImage, localSelectedBlinkImageName, localNewStateName)
            }
        }

        var expanded by remember { mutableStateOf(false) }
        val predefinedStates = remember { listOf("idle", "talking", "listening", "shocked", "crying") }

        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
            TextField(
                modifier = Modifier.menuAnchor().fillMaxWidth()
                    .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["nameField"] = true }
                    .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["nameField"] = false },
                value = localNewStateName, onValueChange = { localNewStateName = it; onStateChange(localSelectedImage, localSelectedImageName, localSelectedBlinkImage, localSelectedBlinkImageName, it) },
                label = { Text("State Name") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                predefinedStates.forEachIndexed { index, selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = { localNewStateName = selectionOption; onStateChange(localSelectedImage, localSelectedImageName, localSelectedBlinkImage, localSelectedBlinkImageName, selectionOption); expanded = false },
                        modifier = Modifier
                            .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["menuItem$index"] = true }
                            .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["menuItem$index"] = false }
                    )
                }
            }
        }

        val isSaveEnabled = localSelectedImage != null && localNewStateName.isNotBlank()

        if (!isSaveEnabled) {
            val missingParts = mutableListOf<String>()
            if (localSelectedImage == null) {
                missingParts.add("an image")
            }
            if (localNewStateName.isBlank()) {
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
                viewModel.createNewState(localNewStateName, localSelectedImage!!, localSelectedImageName, localSelectedBlinkImage, localSelectedBlinkImageName)
                onStateChange(null, "", null, "", "")
            },
            enabled = isSaveEnabled,
            modifier = Modifier.padding(top = 8.dp)
                .onPointerEvent(PointerEventType.Enter) { isHoveringOnItems["save"] = true }
                .onPointerEvent(PointerEventType.Exit) { isHoveringOnItems["save"] = false }
        ) { Text("Save Local State") }
    }
}
