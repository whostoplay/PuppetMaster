package org.menagerie.puppet_master.states

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.decodeToImageBitmap
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.menagerie.puppet_master.ImagePickerDialog
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.Strings

/**
 * A composable that provides a UI for creating and editing puppet states.
 * It allows selecting main and blink images, naming the state, and saving it to the view model.
 *
 * @param modifier The modifier to be applied to the composable.
 * @param viewModel The view model that this composable will interact with.
 * @param onActiveChange A callback that is invoked when the component's interaction state changes.
 * @param onFocusChange A callback that is invoked when the component's focus state changes.
 * @param rootFocusRequester The focus requester for the root composable.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StateCreation(
    modifier: Modifier,
    viewModel: MainViewModel,
    onActiveChange: (Boolean) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester
) {
    val uiState by viewModel.uiState.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var showFilePicker by remember { mutableStateOf(false) }
    var pickingFor by remember { mutableStateOf<String?>(null) }
    var imageWasManuallySelected by remember { mutableStateOf(false) }

    ImagePickerDialog(
        show = showFilePicker,
        title = String.format(Strings.getString(Strings.Keys.SELECT_PART_TITLE), pickingFor ?: Strings.getString(Strings.Keys.SELECT_IMAGES)),
        multiSelect = pickingFor == null,
        initialDirectory = settings.lastImageFolder,
        onCancel = { showFilePicker = false },
        onResult = { images ->
            if (images.isNotEmpty()) {
                imageWasManuallySelected = true
                when (pickingFor) {
                    "main" -> {
                        val (bytes, name) = images.first()
                        viewModel.onStateCreationChange(
                            image = bytes,
                            imageName = name,
                            blinkImage = uiState.selectedBlinkImage,
                            blinkImageName = uiState.selectedBlinkImageName,
                            stateName = uiState.newStateName
                        )
                    }
                    "blink" -> {
                        val (bytes, name) = images.first()
                        viewModel.onStateCreationChange(
                            image = uiState.selectedImage,
                            imageName = uiState.selectedImageName,
                            blinkImage = bytes,
                            blinkImageName = name,
                            stateName = uiState.newStateName
                        )
                    }
                    else -> { // Default behavior: select both
                        val mainImage = images.getOrNull(0)
                        val blinkImage = images.getOrNull(1)
                        viewModel.onStateCreationChange(
                            image = mainImage?.first,
                            imageName = mainImage?.second ?: "",
                            blinkImage = blinkImage?.first,
                            blinkImageName = blinkImage?.second ?: "",
                            stateName = uiState.newStateName
                        )
                    }
                }
            }
            showFilePicker = false
            pickingFor = null
        },
        onFolderSelected = { viewModel.setLastImageFolder(it) }
    )

    Column(modifier = modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(Strings.getString(Strings.Keys.CREATE_NEW_STATE), style = MaterialTheme.typography.titleMedium)
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            uiState.selectedImage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Strings.getString(Strings.Keys.MAIN_IMAGE))
                    Image(
                        bitmap = it.decodeToImageBitmap(),
                        contentDescription = Strings.getString(Strings.Keys.SELECTED_IMAGE),
                        modifier = Modifier
                            .size(100.dp)
                            .combinedClickable(
                                onDoubleClick = {
                                    pickingFor = "main"
                                    showFilePicker = true
                                }
                            ) {}
                    )
                }
            }

            if (uiState.selectedImage != null && uiState.selectedBlinkImage != null) {
                Button(
                    onClick = { viewModel.onStateCreationChange(uiState.selectedBlinkImage, uiState.selectedBlinkImageName, uiState.selectedImage, uiState.selectedImageName, uiState.newStateName) }
                ) {
                    Text(Strings.getString(Strings.Keys.SWAP_IMAGES))
                }
            }

            uiState.selectedBlinkImage?.let {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Strings.getString(Strings.Keys.BLINK_IMAGE))
                    Image(
                        bitmap = it.decodeToImageBitmap(),
                        contentDescription = Strings.getString(Strings.Keys.SELECTED_BLINK_IMAGE),
                        modifier = Modifier
                            .size(100.dp)
                            .combinedClickable(
                                onDoubleClick = {
                                    pickingFor = "blink"
                                    showFilePicker = true
                                }
                            ) {}
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                pickingFor = null
                showFilePicker = true
            }) {
                Text(Strings.getString(Strings.Keys.SELECT_IMAGES))
            }
            if (uiState.selectedImage != null && uiState.selectedBlinkImage == null) {
                Button(onClick = {
                    pickingFor = "blink"
                    showFilePicker = true
                }) {
                    Text(Strings.getString(Strings.Keys.ADD_BLINKING_STATE))
                }
            }
        }

        val textFieldInteractionSource = remember { MutableInteractionSource() }
        val menuInteractionSource = remember { MutableInteractionSource() }
        val isTextFieldHovered by textFieldInteractionSource.collectIsHoveredAsState()
        val isMenuHovered by menuInteractionSource.collectIsHoveredAsState()
        val isHovered = isTextFieldHovered || isMenuHovered

        var isFocused by remember { mutableStateOf(false) }
        var isDropdownExpanded by remember { mutableStateOf(false) }

        val isPanelActive = isHovered || isFocused || isDropdownExpanded

        LaunchedEffect(isPanelActive) {
            onActiveChange(isPanelActive)
        }
        LaunchedEffect(isFocused) {
            onFocusChange(isFocused)
        }

        val predefinedStates = remember { listOf("idle", "talking", "listening", "shocked", "crying") }
        val customStates = remember(activePuppet) {
            activePuppet?.states?.map { it.name }?.filter { it !in predefinedStates } ?: emptyList()
        }

        ExposedDropdownMenuBox(
            expanded = isDropdownExpanded,
            onExpandedChange = { expanded ->
                if (isFocused) {
                    isDropdownExpanded = expanded
                }
            },
        ) {
            TextField(
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
                    .hoverable(interactionSource = textFieldInteractionSource)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (!focusState.isFocused) {
                            isDropdownExpanded = false
                            rootFocusRequester.requestFocus()
                        }
                    },
                value = uiState.newStateName,
                onValueChange = {
                    viewModel.onStateCreationChange(
                        image = uiState.selectedImage,
                        imageName = uiState.selectedImageName,
                        blinkImage = uiState.selectedBlinkImage,
                        blinkImageName = uiState.selectedBlinkImageName,
                        stateName = it
                    )
                },
                label = { Text(Strings.getString(Strings.Keys.STATE_NAME)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                colors = ExposedDropdownMenuDefaults.textFieldColors(),
                singleLine = true,
                keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        rootFocusRequester.requestFocus()
                    }
                )
            )
            ExposedDropdownMenu(
                expanded = isDropdownExpanded,
                onDismissRequest = { isDropdownExpanded = false },
                modifier = Modifier.hoverable(interactionSource = menuInteractionSource)
            ) {
                (predefinedStates + customStates).distinct().forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption) },
                        onClick = {
                            coroutineScope.launch {
                                val selectedState = activePuppet?.states?.find { it.name == selectionOption }

                                if (selectedState?.imageName?.isNotBlank() == true) {
                                    imageWasManuallySelected = false
                                    val mainImage = selectedState.imageName?.let { viewModel.getImageData(it) }
                                    val blinkImage = selectedState.blinkImageName?.let { viewModel.getImageData(it) }
                                    viewModel.onStateCreationChange(
                                        image = mainImage,
                                        imageName = selectedState.imageName,
                                        blinkImage = blinkImage,
                                        blinkImageName = selectedState.blinkImageName ?: "",
                                        stateName = selectionOption
                                    )
                                } else {
                                    if (imageWasManuallySelected) {
                                        viewModel.onStateCreationChange(
                                            image = uiState.selectedImage,
                                            imageName = uiState.selectedImageName,
                                            blinkImage = uiState.selectedBlinkImage,
                                            blinkImageName = uiState.selectedBlinkImageName,
                                            stateName = selectionOption
                                        )
                                    } else {
                                        viewModel.onStateCreationChange(
                                            image = null,
                                            imageName = "",
                                            blinkImage = null,
                                            blinkImageName = "",
                                            stateName = selectionOption
                                        )
                                    }
                                }
                            }
                            isDropdownExpanded = false
                        }
                    )
                }
            }
        }

        val isSaveEnabled = uiState.selectedImage != null && uiState.newStateName.isNotBlank()

        if (!isSaveEnabled) {
            val missingParts = mutableListOf<String>()
            if (uiState.selectedImage == null) {
                missingParts.add(Strings.getString(Strings.Keys.MAIN_IMAGE).lowercase())
            }
            if (uiState.newStateName.isBlank()) {
                missingParts.add(Strings.getString(Strings.Keys.STATE_NAME).lowercase())
            }
            Text(
                String.format(Strings.getString(Strings.Keys.MISSING_ERROR_MESSAGE), missingParts.joinToString(" and ")),
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
        ) { Text(Strings.getString(Strings.Keys.SAVE_UPDATE_STATE)) }
    }
}
