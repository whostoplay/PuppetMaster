package org.menagerie.puppet_master

import androidx.compose.ui.input.key.Key
import kotlinx.serialization.Serializable
import org.menagerie.puppet_master.Hotkey

@Serializable
data class SettingsModel(
    val serverIpAddress: String = "127.0.0.1",
    val fullscreen: Boolean = false,
    val toggleListenHotkey: Hotkey = Hotkey(Key.M.keyCode),
    val togglePublishingHotkey: Hotkey = Hotkey(Key.P.keyCode),
    val toggleOnlineHotkey: Hotkey = Hotkey(Key.O.keyCode),
    val toggleFocusHotkey: Hotkey = Hotkey(Key.F.keyCode),
    val checkAudienceHotkey: Hotkey = Hotkey(Key.A.keyCode, hold = true),
    val toggleControlsHotkey: Hotkey = Hotkey(Key.H.keyCode),
    val startOffline: Boolean = true,
    val lastTroupeFile: String? = null,
    val lastPuppetExportFolder: String? = null,
    val lastImageFolder: String? = null
)
