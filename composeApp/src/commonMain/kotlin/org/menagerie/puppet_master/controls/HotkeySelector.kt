package org.menagerie.puppet_master.controls

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filterIsInstance
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.localisation.Strings

/**
 * A composable for selecting a hotkey combination.
 *
 * It displays the current hotkey and allows the user to change it by clicking the text field and pressing a new key combination.
 * It also provides a switch to toggle between 'hold' and 'toggle' behavior for the hotkey.
 *
 * @param label The label to display for the hotkey selector.
 * @param hotkey The current [Hotkey] to display and edit.
 * @param modifier The modifier to be applied to the composable.
 * @param horizontalArrangement The horizontal arrangement of the label and text field.
 * @param onHotkeyChanged Callback that is invoked when the hotkey is changed.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HotkeySelector(
    label: String,
    hotkey: Hotkey,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.SpaceBetween,
    splitLevel: Boolean = false,
    onHotkeyChanged: (Hotkey) -> Unit,
) {
    var isEditing by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val interactionSource = remember { MutableInteractionSource() }

    LaunchedEffect(interactionSource) {
        interactionSource.interactions
            .filterIsInstance<PressInteraction.Release>()
            .collect { isEditing = true }
    }

    if (isEditing) {
        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
        }
    }

    if(splitLevel) {
        Column (
            modifier = modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = horizontalArrangement
            ) {
                Text(label)
                TextField(
                    value = if (isEditing) Strings.getString(Strings.Keys.PRESS_ANY_KEY) else if (hotkey.key == 4294967296) Strings.getString(Strings.Keys.NOT_ASSIGNED) else hotkey.toString(),
                    onValueChange = {},
                    readOnly = true,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                isEditing = false
                            }
                        }
                        .width(150.dp)
                        .onKeyEvent { event ->
                            if (isEditing) {
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.Escape -> {
                                            onHotkeyChanged(Hotkey(Key.Unknown.keyCode))
                                            focusManager.clearFocus()
                                        }

                                        Key.ShiftLeft,
                                        Key.ShiftRight,
                                        Key.AltLeft,
                                        Key.AltRight,
                                        Key.CtrlLeft,
                                        Key.CtrlRight -> {
                                            //NO OP, Modifiers
                                        }

                                        else -> {
                                            onHotkeyChanged(
                                                hotkey.copy(
                                                    key = event.key.keyCode,
                                                    isShiftPressed = event.isShiftPressed,
                                                    isCtrlPressed = event.isCtrlPressed,
                                                    isAltPressed = event.isAltPressed
                                                )
                                            )
                                            focusManager.clearFocus()
                                        }
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp).weight(.5f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Strings.getString(Strings.Keys.TOGGLE))
                Switch(
                    checked = hotkey.hold,
                    onCheckedChange = { onHotkeyChanged(hotkey.copy(hold = it)) })
                Text(Strings.getString(Strings.Keys.HOLD))
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.padding(top = 8.dp).weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = horizontalArrangement
            ) {
                Text(label)
                TextField(
                    value = if (isEditing) Strings.getString(Strings.Keys.PRESS_ANY_KEY) else if (hotkey.key == 4294967296) Strings.getString(
                        Strings.Keys.NOT_ASSIGNED
                    ) else hotkey.toString(),
                    onValueChange = {},
                    readOnly = true,
                    interactionSource = interactionSource,
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused) {
                                isEditing = false
                            }
                        }
                        .width(150.dp)
                        .onKeyEvent { event ->
                            if (isEditing) {
                                if (event.type == KeyEventType.KeyDown) {
                                    when (event.key) {
                                        Key.Escape -> {
                                            onHotkeyChanged(Hotkey(Key.Unknown.keyCode))
                                            focusManager.clearFocus()
                                        }

                                        Key.ShiftLeft,
                                        Key.ShiftRight,
                                        Key.AltLeft,
                                        Key.AltRight,
                                        Key.CtrlLeft,
                                        Key.CtrlRight -> {
                                            //NO OP, Modifiers
                                        }

                                        else -> {
                                            onHotkeyChanged(
                                                hotkey.copy(
                                                    key = event.key.keyCode,
                                                    isShiftPressed = event.isShiftPressed,
                                                    isCtrlPressed = event.isCtrlPressed,
                                                    isAltPressed = event.isAltPressed
                                                )
                                            )
                                            focusManager.clearFocus()
                                        }
                                    }
                                }
                                true
                            } else {
                                false
                            }
                        }
                )
            }
            Row(
                modifier = Modifier.padding(top = 8.dp).weight(.5f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Strings.getString(Strings.Keys.TOGGLE))
                Switch(
                    checked = hotkey.hold,
                    onCheckedChange = { onHotkeyChanged(hotkey.copy(hold = it)) })
                Text(Strings.getString(Strings.Keys.HOLD))
            }
        }
    }
}
