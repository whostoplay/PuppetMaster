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
import org.menagerie.puppet_master.Constants.UI.Hotkeys

/**
 * Represents a hotkey combination for triggering actions.
 * This class stores the key code and modifier states (Shift, Ctrl, Alt).
 * It can check if a given [KeyEvent] matches the hotkey definition, and supports both press-and-release
 * and press-and-hold activation types.
 *
 * @property key The key code of the hotkey. See [androidx.compose.ui.input.key.Key].
 * @property isShiftPressed `true` if the Shift key must be pressed.
 * @property isCtrlPressed `true` if the Ctrl key must be pressed.
 * @property isAltPressed `true` if the Alt key must be pressed.
 * @property hold `true` if the hotkey is a "hold" type, meaning it's active while the key is down and deactivates on release. If `false`, it triggers only on the initial key down event.
 * @property isDown Internal state to track if the key is currently being held down. This is used to correctly handle `hold` and single-press events.
 */
@Serializable
data class Hotkey(
    val key: Long,
    val isShiftPressed: Boolean = false,
    val isCtrlPressed: Boolean = false,
    val isAltPressed: Boolean = false,
    val hold: Boolean = false,
    var isDown: Boolean = false,
) {
    /**
     * Checks if the given [KeyEvent] matches this hotkey definition.
     * It handles the logic for both single-press and hold-to-activate hotkeys.
     *
     * @param keyEvent The [KeyEvent] to check.
     * @return `true` if the event matches the hotkey activation criteria, `false` otherwise.
     */
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

    fun shallowEquals(other: Hotkey?) : Boolean {
        other?.let {
            return (this.key == other.key &&
                    this.isAltPressed == other.isAltPressed &&
                    this.isCtrlPressed == other.isCtrlPressed &&
                    this.isShiftPressed == other.isShiftPressed
                    )
        }
        return false
    }

    /**
     * Returns a human-readable string representation of the hotkey.
     * For example, "A + CTRL + SHIFT".
     */
    override fun toString(): String {
        val parts = mutableListOf<String>()
        parts.add(Key(key).toString().substringAfter("Key: "))
        if (isCtrlPressed) parts.add(Hotkeys.CTRL)
        if (isAltPressed) parts.add(Hotkeys.ALT)
        if (isShiftPressed) parts.add(Hotkeys.SHIFT)
        return parts.joinToString(Hotkeys.SEPARATOR)
    }
}
