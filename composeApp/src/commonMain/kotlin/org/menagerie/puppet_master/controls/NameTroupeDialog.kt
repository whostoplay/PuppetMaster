package org.menagerie.puppet_master.controls

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Strings

/**
 * A dialog that prompts the user to name a new troupe.
 *
 * This dialog is displayed when a new puppet is created and there is no existing troupe.
 * It includes a text field for the troupe name and buttons to confirm or cancel.
 *
 * @param onConfirm Callback that is invoked with the new troupe name when the user confirms.
 * @param onDismiss Callback that is invoked when the user dismisses the dialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameTroupeDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var troupeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.getString(Strings.Keys.NAME_YOUR_TROUPE_TITLE)) },
        text = {
            Column {
                Text(Strings.getString(Strings.Keys.NAME_YOUR_TROUPE_TEXT))
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = troupeName,
                    onValueChange = { troupeName = it },
                    label = { Text(Strings.getString(Strings.Keys.TROUPE_NAME_LABEL)) },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(troupeName) },
                enabled = troupeName.isNotBlank()
            ) {
                Text(Strings.getString(Strings.Keys.SAVE_BUTTON))
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text(Strings.getString(Strings.Keys.CANCEL_BUTTON))
            }
        }
    )
}
