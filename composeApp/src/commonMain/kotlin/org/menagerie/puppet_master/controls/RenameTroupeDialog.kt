package org.menagerie.puppet_master.controls

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.menagerie.puppet_master.localisation.Strings

/**
 * A dialog that prompts the user to rename a troupe.
 *
 * @param currentName The current name of the troupe.
 * @param onConfirm Callback that is invoked with the new troupe name when the user confirms.
 * @param onDismiss Callback that is invoked when the user dismisses the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RenameTroupeDialog(
    currentName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newTroupeName by remember { mutableStateOf(currentName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.getString(Strings.Keys.RENAME_TROUPE_TITLE)) },
        text = {
            TextField(
                value = newTroupeName,
                onValueChange = { newTroupeName = it },
                label = { Text(Strings.getString(Strings.Keys.NEW_TROUPE_NAME_LABEL)) },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newTroupeName) },
                enabled = newTroupeName.isNotBlank() && newTroupeName != currentName
            ) {
                Text(Strings.getString(Strings.Keys.RENAME_BUTTON))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(Strings.getString(Strings.Keys.CANCEL_BUTTON))
            }
        }
    )
}
