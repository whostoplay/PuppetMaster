package org.menagerie.puppet_master

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.Constants.UI.PortraitLayout
import org.menagerie.puppet_master.controls.ColorPicker
import org.menagerie.puppet_master.controls.ControlDrawer
import org.menagerie.puppet_master.controls.ModeControls
import org.menagerie.puppet_master.controls.PuppetControls
import org.menagerie.puppet_master.controls.ServerControls
import org.menagerie.puppet_master.controls.SpecialEffectsUI
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.localisation.Strings.Keys.CREATE_OR_SELECT_PUPPET
import org.menagerie.puppet_master.localisation.Strings.Keys.EYE_CONTACT_BUTTON
import org.menagerie.puppet_master.localisation.Strings.Keys.PUPPET_CONTROLS_BUTTON
import org.menagerie.puppet_master.localisation.Strings.Keys.SETTINGS_BUTTON
import org.menagerie.puppet_master.localisation.Strings.Keys.STATES_TITLE
import org.menagerie.puppet_master.localisation.Strings.Keys.STATE_CONTROLS_BUTTON
import org.menagerie.puppet_master.localisation.Strings.Keys.STATE_GRAPH_BUTTON
import org.menagerie.puppet_master.navigation.EyeContactScreen
import org.menagerie.puppet_master.navigation.NodeEditorScreen
import org.menagerie.puppet_master.navigation.SettingsScreen
import org.menagerie.puppet_master.state_machine.states.StateCreation
import org.menagerie.puppet_master.state_machine.states.StateEditor

/**
 * The main layout for the application when in portrait orientation.
 *
 * @param viewModel The [MainViewModel] that holds the application state.
 * @param onHover A lambda to be invoked when the user hovers over a component.
 * @param onFocusChange A lambda to be invoked when the focus state of a component changes.
 * @param onShowPuppetImportPickerChange A lambda to be invoked when the puppet import picker should be shown.
 * @param onShowTroupeLoadPickerChange A lambda to be invoked when the troupe load picker should be shown.
 * @param onShowPuppetExportSaverChange A lambda to be invoked when the puppet export saver should be shown.
 * @param rootFocusRequester The [FocusRequester] for the root composable.
 */
@Composable
fun PortraitLayout(
    viewModel: MainViewModel,
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

    var showLeftDrawer by remember { mutableStateOf(false) }
    var showRightDrawer by remember { mutableStateOf(false) }

    ControlDrawer(
        show = showLeftDrawer,
        onDismissRequest = { showLeftDrawer = false }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            item {
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
                    onActiveChange = { onHover(PortraitLayout.PUPPET_CONTROLS_ID, it) },
                    onFocusChange = { onFocusChange(PortraitLayout.PUPPET_CONTROLS_ID, it) },
                    rootFocusRequester = rootFocusRequester
                )
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                ModeControls(operatingMode, viewModel::setOperatingMode, controlMode, viewModel::setControlMode)
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                ServerControls(
                    operatingMode,
                    isPublishing,
                    isListening,
                    { viewModel.setPublishing(it) },
                    { viewModel.toggleListening() }
                )
            }
            if (isListening) {
                item {
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
            }
            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }
            item {
                ColorPicker(onColorSelected = { viewModel.setBackgroundColor(it) })
            }
        }
    }

    ControlDrawer(
        show = showRightDrawer,
        onDismissRequest = { showRightDrawer = false }
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            val currentPuppet = activePuppet
            val currentTroupe = troupe
            if (currentPuppet != null && currentTroupe != null) {
                item {
                    StateCreation(
                        modifier = Modifier.fillMaxWidth(),
                        viewModel = viewModel,
                        onActiveChange = { onHover(PortraitLayout.STATE_CREATION_ID, it) },
                        onFocusChange = { onFocusChange(PortraitLayout.STATE_CREATION_ID, it) },
                        rootFocusRequester = rootFocusRequester
                    )
                }
                item { HorizontalDivider() }
                item { Text(text = Strings.getString(STATES_TITLE)) }
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
                item {
                    SpecialEffectsUI(
                        specialEffectsManager = currentTroupe.specialEffectsManager,
                        onSpecialEffectsManagerChanged = viewModel::onSpecialEffectsManagerChanged,
                        activePuppet = currentPuppet,
                        uploadsDir = viewModel.uploadsDir,
                        preserveState = uiState.preserveState,
                        onPreserveStateChanged = viewModel::onPreserveStateChanged,
                        window = null,
                        onFocusChange = { onFocusChange(PortraitLayout.SPECIAL_EFFECTS_ID, it) },
                        rootFocusRequester = rootFocusRequester,
                        backgroundColor = uiState.backgroundColor
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier.fillParentMaxSize().padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(Strings.getString(CREATE_OR_SELECT_PUPPET))
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { showLeftDrawer = true }) { Text(Strings.getString(PUPPET_CONTROLS_BUTTON)) }
            Button(onClick = { showRightDrawer = true }) { Text(Strings.getString(STATE_CONTROLS_BUTTON)) }
            Button(onClick = { navigator.push(NodeEditorScreen(viewModel)) }) { Text(Strings.getString(STATE_GRAPH_BUTTON)) }
        }
    }
    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = { navigator.push(EyeContactScreen(activePuppet, viewModel)) }) { Text(
            Strings.getString(EYE_CONTACT_BUTTON)) }
        Button(onClick = { navigator.push(SettingsScreen(viewModel)) }) { Text(Strings.getString(SETTINGS_BUTTON)) }
    }
}
