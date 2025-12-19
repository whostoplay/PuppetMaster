package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.Strings
import org.menagerie.puppet_master.controls.HotkeySelector
import org.menagerie.puppet_master.isDesktop


/**
 * A screen that allows the user to configure various application settings.
 */
class SettingsScreen(
    @Transient private val viewModel: MainViewModel
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settings by viewModel.settings.collectAsState()
        var languageMenuExpanded by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title by remember(settings.language) {
                            mutableStateOf(Strings.getString(Strings.Keys.SETTINGS_TITLE))
                        }
                        Text(title)
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = Strings.getString(Strings.Keys.BACK))
                        }
                    }
                )
            }
        ) { innerPadding ->
            Row {
                Column(modifier = Modifier.padding(innerPadding).padding(16.dp).weight(1f)) {
                    val ipLabel = Strings.getString(Strings.Keys.SERVER_IP_ADDRESS)
                    TextField(
                        value = settings.serverIpAddress,
                        onValueChange = {
                            viewModel.updateSettings(settings.copy(serverIpAddress = it))
                        },
                        label = { Text(ipLabel) },
                        modifier = Modifier.fillMaxWidth()
                    )



                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(Strings.getString(Strings.Keys.START_OFFLINE))
                        Checkbox(
                            checked = settings.startOffline,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(startOffline = it)) }
                        )
                    }

                    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        ExposedDropdownMenuBox(
                            expanded = languageMenuExpanded,
                            onExpandedChange = {
                                // This lambda is called when the user clicks the TextField or outside of it.
                                // It correctly toggles the menu's visibility.
                                languageMenuExpanded = !languageMenuExpanded
                            }
                        ) {
                            val languageLabel = Strings.getString(Strings.Keys.LANGUAGE)
                            // 1. The visible part of the dropdown menu (the anchor)
                            TextField(
                                value = settings.language.name, // Display the current language name
                                onValueChange = {}, // The value is read-only, changed by the menu items
                                readOnly = true,
                                label = { Text(languageLabel) },
                                // This modifier is required for ExposedDropdownMenuBox to work correctly
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                // Add a trailing icon to indicate it's a dropdown
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageMenuExpanded)
                                }
                            )

                            // 2. The menu that appears when 'expanded' is true
                            ExposedDropdownMenu(
                                expanded = languageMenuExpanded,
                                // This is called when the user clicks outside the menu to close it
                                onDismissRequest = { languageMenuExpanded = false }
                            ) {
                                // Iterate through all available languages and create a menu item for each
                                Strings.Language.values().forEach { language ->
                                    DropdownMenuItem(
                                        text = { Text(language.name) },
                                        onClick = {
                                            // When an item is clicked:
                                            // 1. Update the settings via the ViewModel
                                            viewModel.updateSettings(settings.copy(language = language))
                                            // 2. Close the menu
                                            languageMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }


                    if (isDesktop()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(Strings.getString(Strings.Keys.FULLSCREEN_ON_STARTUP))
                            Checkbox(
                                checked = settings.fullscreen,
                                onCheckedChange = {
                                    viewModel.updateSettings(
                                        settings.copy(
                                            fullscreen = it
                                        )
                                    )
                                }
                            )
                        }

                        HotkeySelector(Strings.getString(Strings.Keys.TOGGLE_LISTEN_HOTKEY), settings.toggleListenHotkey) {
                            viewModel.updateSettings(settings.copy(toggleListenHotkey = it))
                        }
                        HotkeySelector(Strings.getString(Strings.Keys.TOGGLE_PUBLISHING_HOTKEY), settings.togglePublishingHotkey) {
                            viewModel.updateSettings(settings.copy(togglePublishingHotkey = it))
                        }
                        HotkeySelector(Strings.getString(Strings.Keys.TOGGLE_ONLINE_HOTKEY), settings.toggleOnlineHotkey) {
                            viewModel.updateSettings(settings.copy(toggleOnlineHotkey = it))
                        }
                        HotkeySelector(Strings.getString(Strings.Keys.CHANGE_FOCUS_HOTKEY), settings.toggleFocusHotkey) {
                            viewModel.updateSettings(settings.copy(toggleFocusHotkey = it))
                        }
                        HotkeySelector(Strings.getString(Strings.Keys.CHECK_AUDIENCE_HOTKEY), settings.checkAudienceHotkey) {
                            viewModel.updateSettings(settings.copy(checkAudienceHotkey = it))
                        }
                        HotkeySelector(Strings.getString(Strings.Keys.TOGGLE_CONTROLS_HOTKEY), settings.toggleControlsHotkey) {
                            viewModel.updateSettings(settings.copy(toggleControlsHotkey = it))
                        }
                    }
                }
                Column(modifier = Modifier.padding(innerPadding).padding(16.dp).weight(1f)) {}
                Column(modifier = Modifier.padding(innerPadding).padding(16.dp).weight(1f)) {}
            }
        }
    }
}
