package org.menagerie.puppet_master.controls

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun EditableText(
    text: String,
    onValueChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester,
) {
    var isEditing by remember { mutableStateOf(false) }
    var editText by remember(text, isEditing) { mutableStateOf(text) }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    val onConfirm = {
        isEditing = false
        onValueChange(editText)
        rootFocusRequester.requestFocus()
    }

    if (isEditing) {
        TextField(
            value = editText,
            onValueChange = { editText = it },
            singleLine = true,
            modifier = Modifier
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (isFocused && !focusState.isFocused) {
                        onConfirm()
                    }
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                },
            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { onConfirm() })
        )
    } else {
        Text(
            text = text,
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { isEditing = true })
            }
        )
    }
}

@Composable
fun EditableFloatText(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    rootFocusRequester: FocusRequester
) {
    var isEditing by remember { mutableStateOf(false) }
    var textValue by remember(value, isEditing) { mutableStateOf(value.toString()) }
    val focusRequester = remember { FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            focusRequester.requestFocus()
        }
    }

    val onConfirm = {
        isEditing = false
        textValue.toFloatOrNull()?.let(onValueChange)
        rootFocusRequester.requestFocus()
    }

    if (isEditing) {
        TextField(
            value = textValue,
            onValueChange = { textValue = it },
            singleLine = true,
            modifier = Modifier
                .width(80.dp)
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    if (isFocused && !focusState.isFocused) {
                        onConfirm()
                    }
                    isFocused = focusState.isFocused
                    onFocusChange(focusState.isFocused)
                },
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Number
            ),
            keyboardActions = KeyboardActions(onDone = { onConfirm() })
        )
    } else {
        Text(
            text = "$label: %.2f".format(value),
            modifier = Modifier.pointerInput(Unit) {
                detectTapGestures(onDoubleTap = { isEditing = true })
            }
        )
    }
}
