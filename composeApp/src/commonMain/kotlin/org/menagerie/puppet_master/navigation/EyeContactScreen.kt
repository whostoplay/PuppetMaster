package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RemoveRedEye
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
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
import org.menagerie.puppet_master.ImagePickerDialog
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.PuppetCharacter
import org.menagerie.puppet_master.SerializableOffset
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.decodeToImageBitmap
import org.menagerie.puppet_master.toOffset
import org.menagerie.puppet_master.toSerializableOffset
import kotlin.math.min
import kotlin.math.roundToInt

expect fun Modifier.combinedEyeGestures(
    onDrag: (dragAmount: Offset) -> Unit,
    onScale: (scaleFactor: Offset) -> Unit,
    onRadiusChange: (dragAmount: Offset) -> Unit
): Modifier
expect fun Modifier.gameScreenGestures(onUpdate: (positionDelta: Offset) -> Unit): Modifier

class EyeContactScreen(
    @Transient private val puppet: PuppetCharacter?,
    @Transient private val viewModel: MainViewModel
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val settings by viewModel.settings.collectAsState()

        var selectedState by remember { mutableStateOf(puppet?.states?.firstOrNull()) }
        var isStateSelectorExpanded by remember { mutableStateOf(false) }
        var showTroupeEyesPopup by remember { mutableStateOf(false) }

        var leftEye by remember { mutableStateOf<Eye?>(null) }
        var rightEye by remember { mutableStateOf<Eye?>(null) }
        var syncEyes by remember { mutableStateOf(false) }
        var followCursor by remember { mutableStateOf(false) }
        var isClosedPreview by remember { mutableStateOf(false) }

        // New state variables
        var focusOnGame by remember { mutableStateOf(false) }
        var checkOnAudience by remember { mutableStateOf(false) }
        var gameScreenLocation by remember { mutableStateOf(Offset(0.5f, 0.5f)) }
        var minBlinkRate by remember { mutableStateOf(100f) }
        var maxBlinkRate by remember { mutableStateOf(5000f) }
        var audienceCheckRate by remember { mutableStateOf(8000f) }
        var audienceCheckDuration by remember { mutableStateOf(1500f) }

        var leftEyeOpenData by remember { mutableStateOf<ByteArray?>(null) }
        var leftEyePupilData by remember { mutableStateOf<ByteArray?>(null) }
        var leftEyeClosedData by remember { mutableStateOf<ByteArray?>(null) }
        var rightEyeOpenData by remember { mutableStateOf<ByteArray?>(null) }
        var rightEyePupilData by remember { mutableStateOf<ByteArray?>(null) }
        var rightEyeClosedData by remember { mutableStateOf<ByteArray?>(null) }

        if (showTroupeEyesPopup) {
            TroupeEyesPopup(
                viewModel = viewModel,
                onDismissRequest = { showTroupeEyesPopup = false },
                onApply = { eyeState ->
                    showTroupeEyesPopup = false
                    scope.launch {
                        leftEye = eyeState.eyes.left
                        rightEye = eyeState.eyes.right
                        followCursor = eyeState.eyes.followCursor
                        focusOnGame = eyeState.eyes.focusOnGame
                        gameScreenLocation = eyeState.eyes.gameScreenLocation.toOffset()
                        audienceCheckRate = eyeState.eyes.audienceCheckRate.toFloat()
                        audienceCheckDuration = eyeState.eyes.audienceCheckDuration.toFloat()

                        val lOpenData = async { eyeState.eyes.left.openState.let { viewModel.getImageData(it) } }
                        val lPupilData = async { eyeState.eyes.left.pupil?.let { viewModel.getImageData(it) } }
                        val lClosedData = async { eyeState.eyes.left.closedState?.let { viewModel.getImageData(it) } }
                        val rOpenData = async { eyeState.eyes.right.openState.let { viewModel.getImageData(it) } }
                        val rPupilData = async { eyeState.eyes.right.pupil?.let { viewModel.getImageData(it) } }
                        val rClosedData = async { eyeState.eyes.right.closedState?.let { viewModel.getImageData(it) } }

                        leftEyeOpenData = lOpenData.await()
                        leftEyePupilData = lPupilData.await()
                        leftEyeClosedData = lClosedData.await()
                        rightEyeOpenData = rOpenData.await()
                        rightEyePupilData = rPupilData.await()
                        rightEyeClosedData = rClosedData.await()
                    }
                }
            )
        }

        LaunchedEffect(selectedState) {
            selectedState?.let { state ->
                minBlinkRate = state.minBlinkRate.toFloat()
                maxBlinkRate = state.maxBlinkRate.toFloat()

                state.eyeState?.let { eyeState ->
                    leftEye = eyeState.eyes.left
                    rightEye = eyeState.eyes.right
                    followCursor = eyeState.eyes.followCursor
                    focusOnGame = eyeState.eyes.focusOnGame
                    checkOnAudience = eyeState.eyes.checkOnAudience
                    gameScreenLocation = eyeState.eyes.gameScreenLocation.toOffset()
                    audienceCheckRate = eyeState.eyes.audienceCheckRate.toFloat()
                    audienceCheckDuration = eyeState.eyes.audienceCheckDuration.toFloat()

                    coroutineScope {
                        val lOpenData = async { eyeState.eyes.left.openState.let { viewModel.getImageData(it) } }
                        val lPupilData = async { eyeState.eyes.left.pupil?.let { viewModel.getImageData(it) } }
                        val lClosedData = async { eyeState.eyes.left.closedState?.let { viewModel.getImageData(it) } }
                        val rOpenData = async { eyeState.eyes.right.openState.let { viewModel.getImageData(it) } }
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
        }


        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Strings.getString(Strings.Keys.EYE_CONTACT_STUDIO_TITLE)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = Strings.getString(Strings.Keys.BACK_BUTTON_CONTENT_DESCRIPTION))
                        }
                    },
                    actions = {
                        IconButton(onClick = { showTroupeEyesPopup = true }) {
                            Icon(Icons.Default.RemoveRedEye, contentDescription = Strings.getString(Strings.Keys.SELECT_EYES_FROM_TROUPE_CONTENT_DESCRIPTION))
                        }
                        Button(onClick = {
                            val state = selectedState
                            val left = leftEye
                            val right = rightEye
                            if (state != null && left != null && right != null) {
                                val newEyePair = EyePair(
                                    left = left,
                                    right = right,
                                    followCursor = followCursor,
                                    focusOnGame = focusOnGame,
                                    gameScreenLocation = gameScreenLocation.toSerializableOffset(),
                                    checkOnAudience = checkOnAudience,
                                    audienceCheckRate = audienceCheckRate.toLong(),
                                    audienceCheckDuration = audienceCheckDuration.toLong()
                                )
                                val updatedState = state.copy(
                                    minBlinkRate = minBlinkRate.toLong(),
                                    maxBlinkRate = maxBlinkRate.toLong(),
                                    eyeState = EyeState(state.name, newEyePair)
                                )
                                viewModel.updatePuppetState(updatedState)
                                navigator.pop()
                            }
                        }) {
                            Text(Strings.getString(Strings.Keys.SAVE_BUTTON))
                        }
                    }
                )
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            ) {
                // Controls Column
                Column(
                    modifier = Modifier.fillMaxHeight().weight(1f).padding(end = 16.dp).verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // State Selector
                    ExposedDropdownMenuBox(
                        expanded = isStateSelectorExpanded,
                        onExpandedChange = { isStateSelectorExpanded = !isStateSelectorExpanded }
                    ) {
                        TextField(
                            value = selectedState?.name ?: Strings.getString(Strings.Keys.SELECT_A_STATE),
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
                                            closedState = lEye.closedState,
                                            position = SerializableOffset(150f, 0f)
                                        )
                                        rightEyeOpenData = leftEyeOpenData
                                        rightEyePupilData = leftEyePupilData
                                        rightEyeClosedData = leftEyeClosedData
                                    }
                                }
                            }
                        )
                        Text(Strings.getString(Strings.Keys.SYNC_EYE_PARTS))
                    }

                    if (leftEye?.pupil != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Checkbox(
                                enabled = !focusOnGame,
                                checked = followCursor,
                                onCheckedChange = { followCursor = it })
                            Text(Strings.getString(Strings.Keys.FOLLOW_CURSOR))
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                enabled = !followCursor,
                                checked = focusOnGame,
                                onCheckedChange = { focusOnGame = it })
                            Text(Strings.getString(Strings.Keys.FOCUS_ON_GAME))
                            Spacer(Modifier.width(8.dp))
                            Checkbox(
                                checked = checkOnAudience,
                                onCheckedChange = { checkOnAudience = it })
                            Text(Strings.getString(Strings.Keys.CHECK_ON_AUDIENCE))
                        }
                    }

                    if (selectedState?.blinkImageName != null) {
                        var blinkRateRange by remember(minBlinkRate, maxBlinkRate) { mutableStateOf(minBlinkRate..maxBlinkRate) }
                        Column(modifier = Modifier.fillMaxWidth(0.8f)) {
                            Text(
                                String.format(
                                    Strings.getString(Strings.Keys.BLINK_RATE_RANGE),
                                    blinkRateRange.start.roundToInt(),
                                    blinkRateRange.endInclusive.roundToInt()
                                )
                            )
                            RangeSlider(
                                value = blinkRateRange,
                                onValueChange = {
                                    blinkRateRange = it
                                    minBlinkRate = it.start
                                    maxBlinkRate = it.endInclusive
                                },
                                valueRange = 16f..10000f,
                            )
                        }
                    }

                    if (checkOnAudience) {
                        LabeledSlider(
                            label = Strings.getString(Strings.Keys.AUDIENCE_CHECK_RATE),
                            value = audienceCheckRate,
                            onValueChange = { audienceCheckRate = it },
                            range = 1000f..20000f
                        )
                        LabeledSlider(
                            label = Strings.getString(Strings.Keys.AUDIENCE_CHECK_DURATION),
                            value = audienceCheckDuration,
                            onValueChange = { audienceCheckDuration = it },
                            range = 500f..5000f
                        )
                    }

                    // Eye Image Selection
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Strings.getString(Strings.Keys.LEFT_EYE))
                            EyePartPicker(Strings.getString(Strings.Keys.IRIS), leftEye?.openState, true, onImageSelected = { images ->
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
                            }, onFolderSelected = { viewModel.setLastImageFolder(it) }, initialDirectory = settings.lastImageFolder)
                            EyePartPicker(Strings.getString(Strings.Keys.PUPIL), leftEye?.pupil, true, onImageSelected = { images ->
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
                            }, onFolderSelected = { viewModel.setLastImageFolder(it) }, initialDirectory = settings.lastImageFolder)
                            EyePartPicker(Strings.getString(Strings.Keys.BLINK), leftEye?.closedState, true, onImageSelected = { images ->
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
                            }, onFolderSelected = { viewModel.setLastImageFolder(it) }, initialDirectory = settings.lastImageFolder)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(Strings.getString(Strings.Keys.RIGHT_EYE))
                            EyePartPicker(Strings.getString(Strings.Keys.IRIS), rightEye?.openState, !syncEyes, onImageSelected = { images ->
                                images.firstOrNull()?.let { (data, name) ->
                                    scope.launch {
                                        viewModel.uploadImageData(name, data)
                                        rightEye = rightEye?.copy(openState = name) ?: Eye(openState = name, position = SerializableOffset(150f,0f))
                                        rightEyeOpenData = data
                                    }
                                }
                            }, onFolderSelected = { viewModel.setLastImageFolder(it) }, initialDirectory = settings.lastImageFolder)
                            EyePartPicker(Strings.getString(Strings.Keys.PUPIL), rightEye?.pupil, !syncEyes, onImageSelected = { images ->
                                images.firstOrNull()?.let { (data, name) ->
                                    scope.launch {
                                        viewModel.uploadImageData(name, data)
                                        rightEye = rightEye?.copy(pupil = name) ?: Eye(openState = "", pupil = name)
                                        rightEyePupilData = data
                                    }
                                }
                            }, onFolderSelected = { viewModel.setLastImageFolder(it) }, initialDirectory = settings.lastImageFolder)
                            EyePartPicker(Strings.getString(Strings.Keys.BLINK), rightEye?.closedState, !syncEyes, onImageSelected = { images ->
                                images.firstOrNull()?.let { (data, name) ->
                                    scope.launch {
                                        viewModel.uploadImageData(name, data)
                                        rightEye = rightEye?.copy(closedState = name) ?: Eye(openState = "", closedState = name)
                                        rightEyeClosedData = data
                                    }
                                }
                            }, onFolderSelected = { viewModel.setLastImageFolder(it) }, initialDirectory = settings.lastImageFolder)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(Strings.getString(Strings.Keys.CLOSED_EYES_PREVIEW))
                        Spacer(modifier = Modifier.width(8.dp))
                        Switch(
                            checked = isClosedPreview,
                            onCheckedChange = { isClosedPreview = it }
                        )
                    }
                }

                // Display Column
                Box(
                    modifier = Modifier.fillMaxHeight().weight(1f).padding(start = 16.dp),
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
                                gameScreenLocation = gameScreenLocation,
                                onGameScreenLocationChange = { gameScreenLocation = it },
                                showGameScreenTarget = focusOnGame
                            )
                        }
                    } else {
                        Text(Strings.getString(Strings.Keys.SELECT_STATE_TO_BEGIN))
                    }
                }
            }
        }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    range: ClosedFloatingPointRange<Float>
) {
    Column(modifier = Modifier.fillMaxWidth(0.8f)) {
        Text("$label: ${value.roundToInt()}ms")
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
        )
    }
}

@Composable
private fun DraggablePreviewSurface(
    imageData: ByteArray,
    leftEye: Eye?,
    leftEyeOpenData: ByteArray?,
    leftEyePupilData: ByteArray?,
    leftEyeClosedData: ByteArray?,
    onLeftEyeUpdate: (Eye) -> Unit,
    rightEye: Eye?,
    rightEyeOpenData: ByteArray?,
    rightEyePupilData: ByteArray?,
    rightEyeClosedData: ByteArray?,
    onRightEyeUpdate: (Eye) -> Unit,
    showClosedEyes: Boolean,
    gameScreenLocation: Offset,
    onGameScreenLocationChange: (Offset) -> Unit,
    showGameScreenTarget: Boolean
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
                    contentDescription = Strings.getString(Strings.Keys.STATE_PREVIEW_CONTENT_DESCRIPTION),
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
                    ) { newEye ->
                        onRightEyeUpdate(newEye)
                    }
                }

                if (showGameScreenTarget) {
                    val markerSize = 24.dp
                    val markerSizePx = with(density) { markerSize.toPx() }
                    Box(
                        modifier = Modifier
                            .offset {
                                // Convert normalized offset to pixel offset within the scaled image box
                                IntOffset(
                                    (gameScreenLocation.x * imageBitmap.width * imageScaleFactor - markerSizePx / 2).roundToInt(),
                                    (gameScreenLocation.y * imageBitmap.height * imageScaleFactor - markerSizePx / 2).roundToInt()
                                )
                            }
                            .size(markerSize)
                            .gameScreenGestures { dragAmount ->
                                val imageWidthPx = imageBitmap.width * imageScaleFactor
                                val imageHeightPx = imageBitmap.height * imageScaleFactor

                                if (imageWidthPx > 0 && imageHeightPx > 0) {
                                    // Normalize the drag amount based on the scaled image size
                                    val normalizedDragX = dragAmount.x / imageWidthPx
                                    val normalizedDragY = dragAmount.y / imageHeightPx

                                    // Add the normalized drag amount to the CURRENT location
                                    val newLocation = gameScreenLocation + Offset(
                                        normalizedDragX,
                                        normalizedDragY
                                    )

                                    // Coerce the new location to stay within the 0f..1f bounds and update the state
                                    onGameScreenLocationChange(
                                        Offset(
                                            x = newLocation.x.coerceIn(0f, 1f),
                                            y = newLocation.y.coerceIn(0f, 1f)
                                        )
                                    )
                                }
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 2.dp.toPx()
                            drawLine(Color.Red, Offset(size.width / 2, 0f), Offset(size.width / 2, size.height), strokeWidth)
                            drawLine(Color.Red, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth)
                        }
                        Text(Strings.getString(Strings.Keys.GAME_SCREEN), color = Color.Red, modifier = Modifier.align(Alignment.BottomCenter).offset(y = markerSize))
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
    initialDirectory: String?,
    onImageSelected: (List<Pair<ByteArray, String>>) -> Unit,
    onFolderSelected: (String) -> Unit
) {
    var showDialog by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (enabled) {
            Button(onClick = { showDialog = true }) {
                Text(partName)
            }
        } else {
            Button(onClick = {}, enabled = false) {
                Text(partName)
            }
        }
        Text(imageName ?: "")
    }

    if (showDialog) {
        ImagePickerDialog(
            show = true,
            title = String.format(Strings.getString(Strings.Keys.SELECT_PART_TITLE), partName),
            multiSelect = false,
            initialDirectory = initialDirectory,
            onCancel = { showDialog = false },
            onResult = {
                onImageSelected(it)
                showDialog = false
            },
            onFolderSelected = onFolderSelected
        )
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
                scaleX = eye.scaleX * imageScaleFactor
                scaleY = eye.scaleY * imageScaleFactor
                transformOrigin = TransformOrigin(0f, 0f)
            }
            .combinedEyeGestures(
                onDrag = { dragAmount ->
                    // Calculate the total effective scale applied to the eye.
                    val effectiveScale = Offset(imageScaleFactor / eye.scaleX, imageScaleFactor/ eye.scaleY)
                    if (effectiveScale.x > 0.0f && effectiveScale.y > 0.0f) {
                        // Divide the screen drag amount by the total scale to get the correct model-space offset.
                        val newPosition = eye.position.toOffset() + Offset(dragAmount.x / effectiveScale.x, dragAmount.y / effectiveScale.y)
                        if (newPosition.x.isFinite() && newPosition.y.isFinite()) {
                            onUpdate(eye.copy(position = newPosition.toSerializableOffset()))
                        }
                    }
                },
                onScale = { dragAmount ->
                    // This part looks correct!
                    val scaleSensitivity = 0.01f
                    val newScaleX = (eye.scaleX + dragAmount.x * scaleSensitivity).coerceAtLeast(0.1f)
                    val newScaleY = (eye.scaleY + dragAmount.y * scaleSensitivity).coerceAtLeast(0.1f)

                    if (newScaleX.isFinite() && newScaleY.isFinite()) {
                        onUpdate(eye.copy(scaleX = newScaleX, scaleY = newScaleY))
                    }
                },
                onRadiusChange = { dragAmount ->
                    // This logic also needs to account for the eye's own scale.
                    val effectiveScale = Offset(imageScaleFactor / eye.scaleX, imageScaleFactor/ eye.scaleY)
                    if (effectiveScale.x > 0.0f && effectiveScale.y > 0.0f) {
                        onUpdate(
                            eye.copy(
                                maxPupilRadiusX = (eye.maxPupilRadiusX + dragAmount.x / effectiveScale.x).coerceAtLeast(1f),
                                maxPupilRadiusY = (eye.maxPupilRadiusY + dragAmount.y / effectiveScale.y).coerceAtLeast(1f)
                            )
                        )
                    }
                }
            )
    ) {
        val openStateBitmap = remember(openStateImage) { openStateImage?.let { decodeToImageBitmap(it) } }
        val closedStateBitmap = remember(closedStateImage) { closedStateImage?.let { decodeToImageBitmap(it) } }
        val pupilBitmap = remember(pupilImage) { pupilImage?.let { decodeToImageBitmap(it) } }

        val density = LocalDensity.current
        val eyeSizeModifier = openStateBitmap?.let {
            Modifier.size(
                width = with(density) { it.width.toDp() },
                height = with(density) { it.height.toDp() }
            )
        } ?: Modifier

        Box(modifier = eyeSizeModifier) {
            if (showClosed) {
                closedStateBitmap?.let {
                    Image(bitmap = it, contentDescription = Strings.getString(Strings.Keys.DRAGGABLE_CLOSED_EYE_CONTENT_DESCRIPTION))
                }
            } else {
                openStateBitmap?.let {
                    Image(bitmap = it, contentDescription = Strings.getString(Strings.Keys.DRAGGABLE_OPEN_EYE_CONTENT_DESCRIPTION))
                }
                pupilBitmap?.let {
                    Image(bitmap = it, contentDescription = Strings.getString(Strings.Keys.DRAGGABLE_PUPIL_CONTENT_DESCRIPTION))
                }
            }

            if(pupilImage != null) {
                Canvas(modifier = Modifier.fillMaxSize()) {
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
