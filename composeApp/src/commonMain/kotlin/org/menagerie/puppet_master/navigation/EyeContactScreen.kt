package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
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
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.PuppetStateInfo
import org.menagerie.puppet_master.decodeToImageBitmap
import org.menagerie.puppet_master.toOffset
import org.menagerie.puppet_master.toSerializableOffset
import kotlin.math.min
import kotlin.math.roundToInt

expect fun Modifier.eyeGestures(onUpdate: (positionDelta: Offset, scaleDelta: Float) -> Unit): Modifier
expect fun Modifier.radiusGestures(onUpdate: (scaleDelta: Offset) -> Unit): Modifier

class EyeContactScreen(
    @Transient private val puppet: PuppetCharacter?,
    @Transient private val viewModel: MainViewModel
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()

        val serverIp by viewModel.serverIpAddress.collectAsState()

        var selectedState by remember { mutableStateOf<PuppetStateInfo?>(null) }
        var isStateSelectorExpanded by remember { mutableStateOf(false) }

        var leftEye by remember { mutableStateOf<Eye?>(null) }
        var rightEye by remember { mutableStateOf<Eye?>(null) }
        var syncEyes by remember { mutableStateOf(false) }
        var followCursor by remember { mutableStateOf(false) }
        var isClosedPreview by remember { mutableStateOf(false) }

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
                followCursor = eyeState.eyes.followCursor
                coroutineScope {
                    val lOpenData = async { eyeState.eyes.left.openState?.let { viewModel.getImageData(it) } }
                    val lPupilData = async { eyeState.eyes.left.pupil?.let { viewModel.getImageData(it) } }
                    val lClosedData = async { eyeState.eyes.left.closedState?.let { viewModel.getImageData(it) } }
                    val rOpenData = async { eyeState.eyes.right.openState?.let { viewModel.getImageData(it) } }
                    val rPupilData = async { eyeState.eyes.right.pupil?.let { viewModel.getImageData(it) } }
                    val rClosedData = async { eyeState.eyes.right.closedState?.let { viewModel.getImageData(it) } }

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
                                viewModel.updateEyeState(
                                    state.name,
                                    EyeState(state.name, EyePair(left, right, followCursor))
                                )
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
                        onCheckedChange = { isChecked ->
                            syncEyes = isChecked
                            if (isChecked) {
                                leftEye?.let { lEye ->
                                    rightEye = rightEye?.copy(
                                        openState = lEye.openState,
                                        pupil = lEye.pupil,
                                        closedState = lEye.closedState
                                    ) ?: Eye(
                                        openState = lEye.openState,
                                        pupil = lEye.pupil,
                                        closedState = lEye.closedState
                                    )
                                    rightEyeOpenData = leftEyeOpenData
                                    rightEyePupilData = leftEyePupilData
                                    rightEyeClosedData = leftEyeClosedData
                                }
                            }
                        }
                    )
                    Text("Sync Eye Parts")
                    if (leftEye?.pupil != null) {
                        Spacer(modifier = Modifier.width(16.dp))
                        Checkbox(
                            checked = followCursor,
                            onCheckedChange = { followCursor = it })
                        Text("Eyes follow cursor")
                    }
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
                                scope.launch {
                                    viewModel.uploadImageData(name, data)
                                    leftEye = leftEye?.copy(openState = name) ?: Eye(openState = name)
                                    leftEyeOpenData = data
                                    if (syncEyes) {
                                        rightEye = rightEye?.copy(openState = name) ?: Eye(openState = name)
                                        rightEyeOpenData = data
                                    }
                                }
                            }
                        }
                        EyePartPicker("Pupil", leftEye?.pupil, true) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch {
                                    viewModel.uploadImageData(name, data)
                                    leftEye = leftEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                    leftEyePupilData = data
                                    if (syncEyes) {
                                        rightEye = rightEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                        rightEyePupilData = data
                                    }
                                }
                            }
                        }
                        EyePartPicker("Closed", leftEye?.closedState, true) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch {
                                    viewModel.uploadImageData(name, data)
                                    leftEye = leftEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                    leftEyeClosedData = data
                                    if (syncEyes) {
                                        rightEye = rightEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                        rightEyeClosedData = data
                                    }
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Right Eye")
                        EyePartPicker("Open", rightEye?.openState, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch {
                                    viewModel.uploadImageData(name, data)
                                    rightEye = rightEye?.copy(openState = name) ?: Eye(openState = name)
                                    rightEyeOpenData = data
                                }
                            }
                        }
                        EyePartPicker("Pupil", rightEye?.pupil, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch {
                                    viewModel.uploadImageData(name, data)
                                    rightEye = rightEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                    rightEyePupilData = data
                                }
                            }
                        }
                        EyePartPicker("Closed", rightEye?.closedState, !syncEyes) { images ->
                            images.firstOrNull()?.let { (data, name) ->
                                scope.launch {
                                    viewModel.uploadImageData(name, data)
                                    rightEye = rightEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                    rightEyeClosedData = data
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Closed Eyes Preview")
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = isClosedPreview,
                        onCheckedChange = { isClosedPreview = it }
                    )
                }

                // Preview Area
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val state = selectedState
                    if (state != null) {
                        var stateImage by remember { mutableStateOf<ByteArray?>(null) }
                        LaunchedEffect(state, isClosedPreview) {

                            val name = if (isClosedPreview) state.blinkImageName ?: state.imageName else state.imageName

                            stateImage = viewModel.getImageData(name)
                        }

                        // Call the dedicated composable here
                        stateImage?.let { imageData ->
                            DraggablePreviewSurface(
                                imageData = imageData,
                                leftEye = leftEye,
                                leftEyeOpenData = leftEyeOpenData,
                                leftEyePupilData = leftEyePupilData,
                                leftEyeClosedData = leftEyeClosedData,
                                onLeftEyeUpdate = { leftEye = it },
                                rightEye = rightEye,
                                rightEyeOpenData = rightEyeOpenData,
                                rightEyePupilData = rightEyePupilData,
                                rightEyeClosedData = rightEyeClosedData,
                                onRightEyeUpdate = { rightEye = it },
                                showClosedEyes = isClosedPreview,
                                followCursor = followCursor
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
    leftEyeClosedData: ByteArray?,
    onLeftEyeUpdate: (Eye) -> Unit,
    rightEye: Eye?,
    rightEyeOpenData: ByteArray?,
    rightEyePupilData: ByteArray?,
    rightEyeClosedData: ByteArray?,
    onRightEyeUpdate: (Eye) -> Unit,
    showClosedEyes: Boolean,
    followCursor: Boolean
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
                    DraggableEye(
                        openStateImage = leftEyeOpenData,
                        closedStateImage = leftEyeClosedData,
                        pupilImage = leftEyePupilData,
                        eye = eye,
                        imageScaleFactor = imageScaleFactor,
                        showClosed = showClosedEyes,
                        followCursor = followCursor
                    ) { newEye ->
                        onLeftEyeUpdate(newEye)
                    }
                }

                rightEye?.let { eye ->
                    DraggableEye(
                        openStateImage = rightEyeOpenData,
                        closedStateImage = rightEyeClosedData,
                        pupilImage = rightEyePupilData,
                        eye = eye,
                        imageScaleFactor = imageScaleFactor,
                        showClosed = showClosedEyes,
                        followCursor = followCursor
                    ) { newEye ->
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
    closedStateImage: ByteArray?,
    pupilImage: ByteArray?,
    eye: Eye,
    imageScaleFactor: Float, // Pass the scale factor
    showClosed: Boolean,
    followCursor: Boolean,
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
                    val newPosition = eye.position.toOffset() + (positionDelta / imageScaleFactor)
                    val newScale = eye.scale * scaleDelta

                    // Final safety check: ensure the results are not NaN or Infinite
                    if (newPosition.x.isFinite() && newPosition.y.isFinite() && newScale.isFinite()) {
                        onUpdate(eye.copy(position = newPosition.toSerializableOffset(), scale = newScale))
                    }
                }
            }
    ) {
        val openStateBitmap = remember(openStateImage) { openStateImage?.let { decodeToImageBitmap(it) } }
        val closedStateBitmap = remember(closedStateImage) { closedStateImage?.let { decodeToImageBitmap(it) } }
        val pupilBitmap = remember(pupilImage) { pupilImage?.let { decodeToImageBitmap(it) } }

        if (showClosed) {
            closedStateBitmap?.let {
                Image(bitmap = it, contentDescription = "Draggable closed eye")
            }
        } else {
            openStateBitmap?.let {
                Image(bitmap = it, contentDescription = "Draggable open eye")
            }
            pupilBitmap?.let {
                Image(bitmap = it, contentDescription = "Draggable pupil")
            }
        }

        if (followCursor && pupilImage != null) {
            openStateBitmap?.let {
                val density = LocalDensity.current
                Canvas(
                    modifier = Modifier
                        .size(
                            width = with(density) { it.width.toDp() },
                            height = with(density) { it.height.toDp() }
                        )
                        .radiusGestures { dragAmount ->
                            if (imageScaleFactor > 0f) {
                                onUpdate(
                                    eye.copy(
                                        maxPupilRadiusX = (eye.maxPupilRadiusX + dragAmount.x / imageScaleFactor).coerceAtLeast(1f),
                                        maxPupilRadiusY = (eye.maxPupilRadiusY + dragAmount.y / imageScaleFactor).coerceAtLeast(1f)
                                    )
                                )
                            }
                        }
                ) {
                    val pupilRadiusX = eye.maxPupilRadiusX
                    val pupilRadiusY = eye.maxPupilRadiusY

                    val eyeCenterX = size.width / 2f
                    val eyeCenterY = size.height / 2f

                    drawOval(
                        color = Color.Red,
                        topLeft = Offset(eyeCenterX - pupilRadiusX, eyeCenterY - pupilRadiusY),
                        size = Size(pupilRadiusX * 2, pupilRadiusY * 2),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }
    }
}
