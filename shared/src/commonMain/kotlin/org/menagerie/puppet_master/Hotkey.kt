package org.menagerie.puppet_master

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import kotlinx.serialization.Serializable

@Serializable
data class Hotkey(
    val key: Long,
    val isShiftPressed: Boolean = false,
    val isCtrlPressed: Boolean = false,
    val isAltPressed: Boolean = false,
    val hold: Boolean = false,
    var isDown: Boolean = false,
) {
    fun isHotkey(keyEvent: KeyEvent) : Boolean {
        val keyMatches = keyEvent.key.keyCode == key &&
                keyEvent.isAltPressed == isAltPressed &&
                keyEvent.isCtrlPressed == isCtrlPressed &&
                keyEvent.isShiftPressed == isShiftPressed

        if (!keyMatches) {
            return false
        }

        val wasDown = isDown
        if (keyEvent.type == KeyEventType.KeyDown) {
            isDown = true
        } else if (keyEvent.type == KeyEventType.KeyUp) {
            isDown = false
        }

        return if (hold) {
            (keyEvent.type == KeyEventType.KeyDown && !wasDown) || (keyEvent.type == KeyEventType.KeyUp && wasDown)
        } else {
            keyEvent.type == KeyEventType.KeyDown && !wasDown
        }
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
