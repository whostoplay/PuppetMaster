package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.Eye
import org.menagerie.puppet_master.EyePair
import org.menagerie.puppet_master.EyeState
import org.menagerie.puppet_master.ImageFilePicker
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.toImageBitmap
import kotlin.math.roundToInt

expect fun Modifier.eyeGestures(onUpdate: (positionDelta: Offset, scaleDelta: Float) -> Unit): Modifier

class EyeContactScreen(
    private val puppet: PuppetCharacter?,
    private val serverIp: String,
    private val onSave: (String, EyeState) -> Unit,
    private val getImageData: suspend (String) -> ByteArray?
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow

        var selectedState by remember { mutableStateOf<PuppetStateInfo?>(null) }
        var isStateSelectorExpanded by remember { mutableStateOf(false) }

        var leftEye by remember { mutableStateOf<Eye?>(null) }
        var rightEye by remember { mutableStateOf<Eye?>(null) }
        var syncEyes by remember { mutableStateOf(false) }

        var leftEyeOpenData by remember { mutableStateOf<ByteArray?>(null) }
        var leftEyePupilData by remember { mutableStateOf<ByteArray?>(null) }
        var leftEyeClosedData by remember { mutableStateOf<ByteArray?>(null) }
        var rightEyeOpenData by remember { mutableStateOf<ByteArray?>(null) }
        var rightEyePupilData by remember { mutableStateOf<ByteArray?>(null) }
        var rightEyeClosedData by remember { mutableStateOf<ByteArray?>(null) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Eye Contact Studio") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        Button(onClick = {
                            val state = selectedState
                            val left = leftEye
                            val right = rightEye
                            if (state != null && left != null && right != null) {
                                onSave(state.name, EyeState(state.name, EyePair(left, right)))
                                navigator.pop()
                            }
                        }) {
                            Text("Save")
                        }
                    }
                )
            }
        ) {
            Column(
                modifier = Modifier.fillMaxSize().padding(it).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // State Selector
                ExposedDropdownMenuBox(
                    expanded = isStateSelectorExpanded,
                    onExpandedChange = { isStateSelectorExpanded = !isStateSelectorExpanded }
                ) {
                    TextField(
                        value = selectedState?.name ?: "Select a State",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isStateSelectorExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isStateSelectorExpanded,
                        onDismissRequest = { isStateSelectorExpanded = false }
                    ) {
                        puppet?.states?.forEach { state ->
                            DropdownMenuItem(
                                text = { Text(state.name) },
                                onClick = {
                                    selectedState = state
                                    isStateSelectorExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = syncEyes,
                        onCheckedChange = { syncEyes = it }
                    )
                    Text("Sync Eye Parts")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Eye Image Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Left Eye")
                        EyePartPicker("Open", leftEye?.openState, true) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                leftEye = leftEye?.copy(openState = name) ?: Eye(openState = name)
                                leftEyeOpenData = data
                                if (syncEyes) {
                                    rightEye = rightEye?.copy(openState = name) ?: Eye(openState = name)
                                    rightEyeOpenData = data
                                }
                            }
                        }
                        EyePartPicker("Pupil", leftEye?.pupil, true) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                leftEye = leftEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                leftEyePupilData = data
                                if (syncEyes) {
                                    rightEye = rightEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                    rightEyePupilData = data
                                }
                            }
                        }
                        EyePartPicker("Closed", leftEye?.closedState, true) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                leftEye = leftEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                leftEyeClosedData = data
                                if (syncEyes) {
                                    rightEye = rightEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                    rightEyeClosedData = data
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Right Eye")
                        EyePartPicker("Open", rightEye?.openState, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                rightEye = rightEye?.copy(openState = name) ?: Eye(openState = name)
                                rightEyeOpenData = data
                            }
                        }
                        EyePartPicker("Pupil", rightEye?.pupil, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                rightEye = rightEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                rightEyePupilData = data
                            }
                        }
                        EyePartPicker("Closed", rightEye?.closedState, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                rightEye = rightEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                rightEyeClosedData = data
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Preview Area
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val state = selectedState
                    if (state != null) {
                        var stateImage by remember { mutableStateOf<ByteArray?>(null) }
                        LaunchedEffect(state) {
                            stateImage = getImageData(state.imageName)
                        }

                        stateImage?.let {
                            Image(it.toImageBitmap(), contentDescription = "State preview", modifier = Modifier.fillMaxSize())
                        }

                        leftEye?.let { eye ->
                            DraggableEye(leftEyeOpenData, leftEyePupilData, eye.position, eye.scale) { newPosition, newScale ->
                                leftEye = eye.copy(position = newPosition, scale = newScale)
                            }
                        }

                        rightEye?.let { eye ->
                            DraggableEye(rightEyeOpenData, rightEyePupilData, eye.position, eye.scale) { newPosition, newScale ->
                                rightEye = eye.copy(position = newPosition, scale = newScale)
                            }
                        }
                    } else {
                        Text("Select a state to begin.")
                    }
                }
            }
        }
    }
}

@Composable
private fun EyePartPicker(
    partName: String,
    imageName: String?,
    enabled: Boolean,
    onImageSelected: (List<Pair<ByteArray, String>>) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (enabled) {
            ImageFilePicker(partName, onImagesSelected = onImageSelected)
        } else {
            Button(onClick = {}, enabled = false) {
                Text(partName)
            }
        }
        Text(imageName ?: "")
    }
}

@Composable
fun DraggableEye(openStateImage: ByteArray?, pupilImage: ByteArray?, position: Offset, scale: Float, onUpdate: (Offset, Float) -> Unit) {// Internal state for the draggable eye to manage its own position and scale during gestures.
    var eyePosition by remember { mutableStateOf(position) }
    var eyeScale by remember { mutableStateOf(scale) }

    // This effect synchronizes the internal state with the external state passed in as parameters.
    // This is useful for when the state is loaded or changed from outside the composable.
    LaunchedEffect(position, scale) {
        eyePosition = position
        eyeScale = scale
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(eyePosition.x.roundToInt(), eyePosition.y.roundToInt()) }
            .graphicsLayer(scaleX = eyeScale, scaleY = eyeScale)
            .eyeGestures { positionDelta, scaleDelta ->
                // Update the internal state directly during the gesture.
                val newPosition = eyePosition + positionDelta
                val newScale = (eyeScale + scaleDelta).coerceIn(0.1f, 5.0f) // Added coercion for robustness
                eyePosition = newPosition
                eyeScale = newScale
                // Inform the parent about the final updated state.
                onUpdate(newPosition, newScale)
            }
    ) {
        openStateImage?.let {
            Image(it.toImageBitmap(), contentDescription = "Eye open state")
        }

        pupilImage?.let {
            Image(it.toImageBitmap(), contentDescription = "Eye pupil")
        }
    }
}