package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.controls.HotkeySelector
import org.menagerie.puppet_master.isDesktop

class SettingsScreen(
    @Transient private val viewModel: MainViewModel
) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val settings by viewModel.settings.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Settings") },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { innerPadding ->
            Row {
                Column(modifier = Modifier.padding(innerPadding).padding(16.dp).weight(1f)) {
                    TextField(
                        value = settings.serverIpAddress,
                        onValueChange = { newValue ->
                            viewModel.updateSettings(settings.copy(serverIpAddress = newValue))
                        },
                        label = { Text("Server IP Address") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Start Offline")
                        Checkbox(
                            checked = settings.startOffline,
                            onCheckedChange = { viewModel.updateSettings(settings.copy(startOffline = it)) }
                        )
                    }

                    if (isDesktop()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Fullscreen on Startup")
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

                        HotkeySelector("Toggle Listen Hotkey", settings.toggleListenHotkey) {
                            viewModel.updateSettings(settings.copy(toggleListenHotkey = it))
                        }
                        HotkeySelector("Toggle Publishing Hotkey", settings.togglePublishingHotkey) {
                            viewModel.updateSettings(settings.copy(togglePublishingHotkey = it))
                        }
                        HotkeySelector("Toggle Online Hotkey", settings.toggleOnlineHotkey) {
                            viewModel.updateSettings(settings.copy(toggleOnlineHotkey = it))
                        }
                        HotkeySelector("Change Focus Hotkey", settings.toggleFocusHotkey) {
                            viewModel.updateSettings(settings.copy(toggleFocusHotkey = it))
                        }
                        HotkeySelector("Check Audience Hotkey", settings.checkAudienceHotkey) {
                            viewModel.updateSettings(settings.copy(checkAudienceHotkey = it))
                        }
                    }
                }
                Column(modifier = Modifier.padding(innerPadding).padding(16.dp).weight(1f)) {}
                Column(modifier = Modifier.padding(innerPadding).padding(16.dp).weight(1f)) {}
            }
        }
    }
}
