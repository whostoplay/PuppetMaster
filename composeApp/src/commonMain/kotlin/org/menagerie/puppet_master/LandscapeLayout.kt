package org.menagerie.puppet_master

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.Constants.UI.LandscapeLayout
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.controls.ColorPicker
import org.menagerie.puppet_master.controls.DraggableSplitter
import org.menagerie.puppet_master.controls.ModeControls
import org.menagerie.puppet_master.controls.PuppetControls
import org.menagerie.puppet_master.controls.ServerControls
import org.menagerie.puppet_master.controls.SpecialEffectsUI
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.navigation.EyeContactScreen
import org.menagerie.puppet_master.navigation.NodeEditorScreen
import org.menagerie.puppet_master.navigation.SettingsScreen
import org.menagerie.puppet_master.state_machine.states.StateCreation
import org.menagerie.puppet_master.state_machine.states.StateEditor

/**
 * The main layout for the application when in landscape orientation or on a desktop device.
 *
 * @param viewModel The [MainViewModel] that holds the application state.
 * @param leftPanelWidth The width of the left panel as a fraction of the total width.
 * @param rightPanelWidth The width of the right panel as a fraction of the total width.
 * @param onLeftPanelResize A lambda to be invoked when the left panel is resized.
 * @param onRightPanelResize A lambda to be invoked when the right panel is resized.
 * @param onHover A lambda to be invoked when the user hovers over a component.
 * @param onFocusChange A lambda to be invoked when the focus state of a component changes.
 * @param onShowPuppetImportPickerChange A lambda to be invoked when the puppet import picker should be shown.
 * @param onShowTroupeLoadPickerChange A lambda to be invoked when the troupe load picker should be shown.
 * @param onShowPuppetExportSaverChange A lambda to be invoked when the puppet export saver should be shown.
 * @param rootFocusRequester The [FocusRequester] for the root composable.
 */
@Composable
fun LandscapeLayout(
    viewModel: MainViewModel,
    leftPanelWidth: Float,
    rightPanelWidth: Float,
    onLeftPanelResize: (Float) -> Unit,
    onRightPanelResize: (Float) -> Unit,
    onHover: (String, Boolean) -> Unit,
    onFocusChange: (String, Boolean) -> Unit,
    onShowPuppetImportPickerChange: (Boolean) -> Unit,
    onShowTroupeLoadPickerChange: (Boolean) -> Unit,
    onShowPuppetExportSaverChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester
) {
    val navigator = LocalNavigator.currentOrThrow
    val troupe by viewModel.troupe.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()
    val operatingMode by viewModel.operatingMode.collectAsState()
    val controlMode by viewModel.controlMode.collectAsState()
    val isPublishing by viewModel.isPublishing.collectAsState()
    val isListening by viewModel.isListening.collectAsState()
    val rawAudioLevel by viewModel.rawAudioLevel.collectAsState()
    val thresholds by viewModel.thresholds.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val selectedState by viewModel.selectedState.collectAsState()

    Row(
        modifier = Modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            modifier = Modifier.fillMaxHeight().weight(leftPanelWidth),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .onHover { onHover(LandscapeLayout.LEFT_PANEL_ID, it) }
            ) {
                PuppetControls(
                    troupe = troupe,
                    activePuppet = activePuppet,
                    onPuppetSelected = { viewModel.setActivePuppet(it) },
                    onPuppetCreated = viewModel::createNewPuppet,
                    onImportPuppet = { onShowPuppetImportPickerChange(true) },
                    onExportPuppet = { onShowPuppetExportSaverChange(true) },
                    onRenameTroupe = viewModel::renameTroupe,
                    onLoadTroupe = { onShowTroupeLoadPickerChange(true) },
                    onNewTroupeCreated = viewModel::createNewTroupe,
                    onActiveChange = { onHover(LandscapeLayout.PUPPET_CONTROLS_ID, it) },
                    onFocusChange = { onFocusChange(LandscapeLayout.PUPPET_CONTROLS_ID, it) },
                    rootFocusRequester = rootFocusRequester
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ModeControls(operatingMode, viewModel::setOperatingMode, controlMode, viewModel::setControlMode)
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ServerControls(
                    operatingMode,
                    isPublishing,
                    isListening,
                    { viewModel.setPublishing(it) },
                    { viewModel.toggleListening() }
                )
                if (isListening) {
                    VolumeIndicator(
                        level = rawAudioLevel,
                        modifier = Modifier.fillMaxWidth().padding(8.dp).size(20.dp),
                        thresholds = thresholds,
                        onAddThreshold = {
                            viewModel.addThreshold(it)
                            viewModel.showStateAssignmentDialog(it)
                        },
                        onUpdateThreshold = viewModel::updateThreshold,
                        onThresholdSelected = { viewModel.showStateAssignmentDialog(it) }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) })
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Button(
                    onClick = { navigator.push(EyeContactScreen(activePuppet, viewModel)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(Strings.getString(Strings.Keys.EYE_CONTACT_BUTTON))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Button(
                    onClick = { navigator.push(NodeEditorScreen(viewModel)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(Strings.getString(Strings.Keys.STATE_GRAPH_BUTTON))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Button(
                    onClick = { navigator.push(SettingsScreen(viewModel)) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(Strings.getString(Strings.Keys.SETTINGS_BUTTON))
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        DraggableSplitter(
            onDelta = { delta ->
                val newWidth = leftPanelWidth + (delta / 1000f) // Adjust this factor as needed
                onLeftPanelResize(newWidth.coerceIn(0.2f, 0.5f))
            },
            modifier = Modifier.onHover { onHover(LandscapeLayout.LEFT_DRAG_ID, it) }
        )

        Spacer(
            modifier = Modifier.fillMaxHeight()
                .weight((1f - leftPanelWidth - rightPanelWidth).coerceAtLeast(0.05f))
        )

        DraggableSplitter(
            onDelta = { delta ->
                val newWidth = rightPanelWidth - (delta / 1000f) // Adjust this factor as needed
                onRightPanelResize(newWidth.coerceIn(0.2f, 0.5f))
            },
            modifier = Modifier.onHover { onHover(LandscapeLayout.RIGHT_DRAG_ID, it) }
        )

        Surface(
            modifier = Modifier.fillMaxHeight().weight(rightPanelWidth),
            color = MaterialTheme.colorScheme.background
        ) {
            LazyColumn(
                modifier = Modifier.onHover { onHover(LandscapeLayout.RIGHT_PANEL_ID, it) }
            ) {
                val currentPuppet = activePuppet
                val currentTroupe = troupe
                if (currentPuppet != null && currentTroupe != null) {
                    item {
                        StateCreation(
                            modifier = Modifier.fillMaxWidth(),
                            viewModel = viewModel,
                            onActiveChange = { onHover(LandscapeLayout.STATE_CREATION_ID, it) },
                            onFocusChange = { onFocusChange(LandscapeLayout.STATE_CREATION_ID, it) },
                            rootFocusRequester = rootFocusRequester
                        )
                    }
                    item { HorizontalDivider() }
                    item { Text(text = Strings.getString(Strings.Keys.STATES_TITLE)) }
                    item { HorizontalDivider() }
                    items(currentPuppet.states) { state ->
                        Text(
                            text = state.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.selectState(state) }
                                .background(if (state == selectedState) MaterialTheme.colorScheme.primaryContainer else Color.Transparent)
                                .padding(8.dp)
                        )
                    }
                    item {
                        selectedState?.let { state ->
                            StateEditor(
                                modifier = Modifier.fillMaxWidth(),
                                selectedState = state,
                                specialEffectsManager = currentTroupe.specialEffectsManager,
                                onStateUpdated = { viewModel.updatePuppetState(it) },
                                onStateHotkeyChanged = viewModel::updateStateHotkey
                            )
                        }
                    }
                    item { Spacer(Modifier.height(64.dp)) }
                    item { HorizontalDivider(thickness = 8.dp) }
                    item { Spacer(Modifier.height(64.dp)) }
                    item {
                        SpecialEffectsUI(
                            specialEffectsManager = currentTroupe.specialEffectsManager,
                            onSpecialEffectsManagerChanged = viewModel::onSpecialEffectsManagerChanged,
                            activePuppet = currentPuppet,
                            uploadsDir = viewModel.uploadsDir,
                            preserveState = uiState.preserveState,
                            onPreserveStateChanged = viewModel::onPreserveStateChanged,
                            window = null,
                            onFocusChange = { onFocusChange(LandscapeLayout.SPECIAL_EFFECTS_ID, it) },
                            rootFocusRequester = rootFocusRequester,
                            backgroundColor = uiState.backgroundColor,
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier.fillParentMaxSize().padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(Strings.getString(Strings.Keys.CREATE_OR_SELECT_PUPPET))
                        }
                    }
                }
            }
        }
    }
}
