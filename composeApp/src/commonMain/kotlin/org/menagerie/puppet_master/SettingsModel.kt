package org.menagerie.puppet_master

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import kotlinx.serialization.Serializable

@Serializable
data class Hotkey(
    val key: Long,
    val isShiftPressed: Boolean = false,
    val isCtrlPressed: Boolean = false,
    val isAltPressed: Boolean = false
) {
    fun isHotkey(keyEvent: KeyEvent) : Boolean {
        return keyEvent.key.keyCode == key
                && keyEvent.isAltPressed == isAltPressed
                && keyEvent.isCtrlPressed == isCtrlPressed
                && keyEvent.isShiftPressed == isShiftPressed
    }

    override fun toString(): String {
        val parts = mutableListOf<String>()
        parts.add(Key(key).toString().substringAfter("Key: "))
        if (isCtrlPressed) parts.add("CTRL")
        if (isAltPressed) parts.add("ALT")
        if (isShiftPressed) parts.add("SHIFT")
        return parts.joinToString(" + ")
    }
}

@Serializable
data class SettingsModel(
    val serverIpAddress: String = "127.0.0.1",
    val fullscreen: Boolean = false,
    val toggleListenHotkey: Hotkey = Hotkey(Key.M.keyCode),
    val togglePublishingHotkey: Hotkey = Hotkey(Key.P.keyCode),
    val toggleOnlineHotkey: Hotkey = Hotkey(Key.O.keyCode),
    val startOffline: Boolean = true
)
