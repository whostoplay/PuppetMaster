package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.menagerie.puppet_master.Eye
import org.menagerie.puppet_master.EyePair
import org.menagerie.puppet_master.EyeState
import org.menagerie.puppet_master.ImageFilePicker
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.decodeToImageBitmap
import kotlin.math.min
import kotlin.math.roundToInt

expect fun Modifier.eyeGestures(onUpdate: (positionDelta: Offset, scaleDelta: Float) -> Unit): Modifier

class EyeContactScreen(
    private val puppet: PuppetCharacter?,
    private val serverIp: String,
    private val onSave: (String, EyeState) -> Unit,
    private val getImageData: suspend (String) -> ByteArray?,
    private val uploadImageData: suspend (String, ByteArray) -> Unit
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

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

        LaunchedEffect(selectedState) {
            selectedState?.eyeState?.let { eyeState ->
                leftEye = eyeState.eyes.left
                rightEye = eyeState.eyes.right
                coroutineScope {
                    val lOpenData = async { eyeState.eyes.left.openState?.let { getImageData(it) } }
                    val lPupilData = async { eyeState.eyes.left.pupil?.let { getImageData(it) } }
                    val lClosedData = async { eyeState.eyes.left.closedState?.let { getImageData(it) } }
                    val rOpenData = async { eyeState.eyes.right.openState?.let { getImageData(it) } }
                    val rPupilData = async { eyeState.eyes.right.pupil?.let { getImageData(it) } }
                    val rClosedData = async { eyeState.eyes.right.closedState?.let { getImageData(it) } }

                    leftEyeOpenData = lOpenData.await()
                    leftEyePupilData = lPupilData.await()
                    leftEyeClosedData = lClosedData.await()
                    rightEyeOpenData = rOpenData.await()
                    rightEyePupilData = rPupilData.await()
                    rightEyeClosedData = rClosedData.await()
                }
            } ?: run {
                leftEye = null
                rightEye = null
                leftEyeOpenData = null
                leftEyePupilData = null
                leftEyeClosedData = null
                rightEyeOpenData = null
                rightEyePupilData = null
                rightEyeClosedData = null
            }
        }

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
                                scope.launch { uploadImageData(name, data) }
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
                                scope.launch { uploadImageData(name, data) }
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
                                scope.launch { uploadImageData(name, data) }
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
                                scope.launch { uploadImageData(name, data) }
                                rightEye = rightEye?.copy(openState = name) ?: Eye(openState = name)
                                rightEyeOpenData = data
                            }
                        }
                        EyePartPicker("Pupil", rightEye?.pupil, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch { uploadImageData(name, data) }
                                rightEye = rightEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                rightEyePupilData = data
                            }
                        }
                        EyePartPicker("Closed", rightEye?.closedState, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch { uploadImageData(name, data) }
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

                        // Call the dedicated composable here
                        stateImage?.let { imageData ->
                            DraggablePreviewSurface(
                                imageData = imageData,
                                leftEye = leftEye,
                                leftEyeOpenData = leftEyeOpenData,
                                leftEyePupilData = leftEyePupilData,
                                onLeftEyeUpdate = { leftEye = it },
                                rightEye = rightEye,
                                rightEyeOpenData = rightEyeOpenData,
                                rightEyePupilData = rightEyePupilData,
                                onRightEyeUpdate = { rightEye = it }
                            )
                        }
                    } else {
                        Text("Select a state to begin.")
                    }
                }

            }
        }
    }
}

/**
 * A dedicated composable responsible for rendering the draggable preview surface.
 * This ensures all Compose-related calls are made from a valid Composable context.
 */
@Composable
private fun DraggablePreviewSurface(
    imageData: ByteArray,
    leftEye: Eye?, leftEyeOpenData: ByteArray?,
    leftEyePupilData: ByteArray?,
    onLeftEyeUpdate: (Eye) -> Unit,
    rightEye: Eye?,
    rightEyeOpenData: ByteArray?,
    rightEyePupilData: ByteArray?,
    onRightEyeUpdate: (Eye) -> Unit
) {
    val imageBitmap = remember(imageData) { decodeToImageBitmap(imageData) }
    val density = LocalDensity.current

    BoxWithConstraints(contentAlignment = Alignment.Center) {
        if (constraints.maxWidth > 0 && constraints.maxHeight > 0 && imageBitmap.width > 0 && imageBitmap.height > 0) {
            val imageScaleFactor = min(
                constraints.maxWidth.toFloat() / imageBitmap.width,
                constraints.maxHeight.toFloat() / imageBitmap.height
            )

            // This Box acts as the scaled canvas for the image and eyes
            Box(
                modifier = Modifier.size(
                    width = with(density) { (imageBitmap.width * imageScaleFactor).toDp() },
                    height = with(density) { (imageBitmap.height * imageScaleFactor).toDp() }
                )
            ) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "State preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                leftEye?.let { eye ->
                    DraggableEye(leftEyeOpenData, leftEyePupilData, eye, imageScaleFactor) { newEye ->
                        onLeftEyeUpdate(newEye)
                    }
                }

                rightEye?.let { eye ->
                    DraggableEye(rightEyeOpenData, rightEyePupilData, eye, imageScaleFactor) { newEye ->
                        onRightEyeUpdate(newEye)
                    }
                }
            }
        }
        // While waiting for valid constraints, this composable will simply be empty for a frame,
        // which is visually unnoticeable but prevents the crash.
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
fun DraggableEye(
    openStateImage: ByteArray?,
    pupilImage: ByteArray?,
    eye: Eye,
    imageScaleFactor: Float, // Pass the scale factor
    onUpdate: (Eye) -> Unit
) {
    Box(
        modifier = Modifier
            .offset {
                // Apply the scale factor for display
                IntOffset(
                    (eye.position.x * imageScaleFactor).roundToInt(),
                    (eye.position.y * imageScaleFactor).roundToInt()
                )
            }
            .graphicsLayer {
                // Apply the scale factor for display
                scaleX = eye.scale * imageScaleFactor
                scaleY = eye.scale * imageScaleFactor
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .eyeGestures { positionDelta, scaleDelta ->
                if (imageScaleFactor > 0.0f && scaleDelta.isFinite() && scaleDelta > 0.0f) {
                    val newPosition = eye.position + (positionDelta / imageScaleFactor)
                    val newScale = eye.scale * scaleDelta

                    // Final safety check: ensure the results are not NaN or Infinite
                    if (newPosition.x.isFinite() && newPosition.y.isFinite() && newScale.isFinite()) {
                        onUpdate(eye.copy(position = newPosition, scale = newScale))
                    }
                }
            }
    ) {
        val openStateBitmap = remember(openStateImage) { openStateImage?.let { decodeToImageBitmap(it) } }
        openStateBitmap?.let {
            Image(bitmap = it, contentDescription = "Draggable open eye")
        }
        val pupilBitmap = remember(pupilImage) { pupilImage?.let { decodeToImageBitmap(it) } }
        pupilBitmap?.let {
            Image(bitmap = it, contentDescription = "Draggable pupil")
        }
    }
}
