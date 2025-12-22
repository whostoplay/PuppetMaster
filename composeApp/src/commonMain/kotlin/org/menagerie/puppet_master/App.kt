package org.menagerie.puppet_master

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.delay
import org.menagerie.puppet_master.Constants.UI.CONTROLS_VISIBILITY_DELAY_MS
import org.menagerie.puppet_master.Constants.UI.DEFAULT_PANEL_WIDTH_FRACTION
import org.menagerie.puppet_master.Constants.UI.FileDialogs
import org.menagerie.puppet_master.Strings.Keys.GETTING_STARTED
import org.menagerie.puppet_master.Strings.Keys.LOAD_STATE_IMAGE
import org.menagerie.puppet_master.controls.FilePicker
import org.menagerie.puppet_master.controls.FileSaver
import org.menagerie.puppet_master.navigation.AppNavigator
import org.menagerie.puppet_master.previews.LivePreview

/**
 * The main entry point for the application's UI.
 */
@Composable
fun App() {
    AppNavigator()
}

/**
 * The main content of the application.
 *
 * This composable is responsible for displaying the live preview, the controls, and handling user input.
 *
 * @param viewModel The [MainViewModel] that holds the application state.
 * @param window The window object, which can be passed to the live preview.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun AppContent(viewModel: MainViewModel, window: Any?) {
    val troupe by viewModel.troupe.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val displayedImageName by viewModel.displayedImageName.collectAsState()
    val activeState by viewModel.activeState.collectAsState()
    val isBlinking by viewModel.isBlinking.collectAsState()
    val animationState by viewModel.animationState.collectAsState()
    val idleImage by viewModel.idleImage.collectAsState()
    val focusRequester = remember { FocusRequester() }

    var showPermissionRequest by remember { mutableStateOf(false) }
    if (showPermissionRequest) {
        RequestAudioPermission { granted ->
            if (granted) {
                viewModel.toggleListening()
            }
            showPermissionRequest = false
        }
    }

    val isDesktop = isDesktop()
    var showControls by remember { mutableStateOf(isDesktop) }
    var controlsLocked by remember { mutableStateOf(!isDesktop) }
    var controlsHiddenByKey by remember { mutableStateOf(false) }
    val isHoveringOn = remember { mutableStateMapOf<String, Boolean>() }
    val isHoveringOnControls = isHoveringOn.values.any { it }
    val controlsVisible = (showControls || controlsLocked) && !controlsHiddenByKey
    val controlsAlpha by animateFloatAsState(if (controlsVisible) 1f else 0f)

    var showPuppetImportPicker by remember { mutableStateOf(false) }
    var showTroupeLoadPicker by remember { mutableStateOf(false) }
    var showPuppetExportSaver by remember { mutableStateOf(false) }
    val textFieldFocusStates = remember { mutableStateMapOf<String, Boolean>() }
    val isTextFieldFocused = textFieldFocusStates.values.any { it }

    FilePicker(show = showPuppetImportPicker, fileExtensions = FileDialogs.PUPPET_FILE_EXTENSIONS) { filePath ->
        if (filePath != null) {
            viewModel.importPuppet(filePath)
        }
        showPuppetImportPicker = false
    }

    FilePicker(show = showTroupeLoadPicker, fileExtensions = FileDialogs.TROUPE_FILE_EXTENSIONS) { filePath ->
        if (filePath != null) {
            viewModel.loadTroupeFromFile(filePath)
        }
        showTroupeLoadPicker = false
    }

    FileSaver(
        show = showPuppetExportSaver,
        defaultFileName = "${activePuppet?.name}.${FileDialogs.PUPPET_FILE_EXTENSION}",
        fileExtensions = FileDialogs.PUPPET_FILE_EXTENSIONS
    ) { filePath ->
        if (filePath != null) {
            activePuppet?.let { viewModel.exportPuppet(it.name, filePath) }
        }
        showPuppetExportSaver = false
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(isHoveringOnControls) {
        if (isHoveringOnControls) {
            showControls = true
        }
    }

    LaunchedEffect(showControls, isHoveringOnControls, controlsLocked) {
        if (showControls && !isHoveringOnControls && !controlsLocked) {
            delay(CONTROLS_VISIBILITY_DELAY_MS)
            if (!isHoveringOnControls && !controlsLocked) {
                showControls = false
            }
        }
    }

    AppDialogs(viewModel)

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { controlsLocked = !controlsLocked },
                    onTap = { showControls = true } // Always show on single tap
                )
            }
            .focusRequester(focusRequester)
            .onKeyEvent { event ->
                handleKeyEvent(
                    event,
                    viewModel,
                    settings,
                    isDesktop,
                    isTextFieldFocused,
                ) { controlsHiddenByKey = !controlsHiddenByKey }
            }
    ) {
        val isLandscape = maxWidth > maxHeight
        var leftPanelWidth by remember { mutableFloatStateOf(DEFAULT_PANEL_WIDTH_FRACTION) }
        var rightPanelWidth by remember { mutableFloatStateOf(DEFAULT_PANEL_WIDTH_FRACTION) }

        if (idleImage != null) {
            key(activePuppet) {
                LivePreview(
                    operatingMode = operatingMode,
                    puppetState = activeState,
                    isBlinking = isBlinking,
                    uploadsDir = viewModel.uploadsDir,
                    backgroundColor = uiState.backgroundColor,
                    serverIp = settings.serverIpAddress,
                    animationState = animationState,
                    window = window,
                    isAudienceCheckForced = viewModel.isAudienceCheckForced.collectAsState().value,
                    displayedImageName = displayedImageName,
                    idleImage = idleImage!!,
                    onFocusPointUpdate = { offset ->
                        viewModel.onNormalizedMousePositionChanged(offset)
                    }
                )
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(if (troupe == null) Strings.getString(GETTING_STARTED) else Strings.getString(LOAD_STATE_IMAGE))
            }
        }

        Box(modifier = Modifier.graphicsLayer(alpha = controlsAlpha).fillMaxSize()) {
            if (isLandscape || isDesktop) {
                LandscapeLayout(
                    viewModel,
                    leftPanelWidth,
                    rightPanelWidth,
                    { newWidth -> leftPanelWidth = newWidth },
                    { newWidth -> rightPanelWidth = newWidth },
                    { id, hovering -> isHoveringOn[id] = hovering },
                    { id, focused -> textFieldFocusStates[id] = focused },
                    { showPuppetImportPicker = it },
                    { showTroupeLoadPicker = it },
                    { showPuppetExportSaver = it },
                    focusRequester
                )
            } else { // Portrait
                PortraitLayout(
                    viewModel,
                    { id, hovering -> isHoveringOn[id] = hovering },
                    { id, focused -> textFieldFocusStates[id] = focused },
                    { showPuppetImportPicker = it },
                    { showTroupeLoadPicker = it },
                    { showPuppetExportSaver = it },
                    focusRequester
                )
            }
        }
    }
}
