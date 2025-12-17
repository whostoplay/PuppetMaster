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
        title = { Text("Rename Troupe") },
        text = {
            TextField(
                value = newTroupeName,
                onValueChange = { newTroupeName = it },
                label = { Text("New Troupe Name") },
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(newTroupeName) },
                enabled = newTroupeName.isNotBlank() && newTroupeName != currentName
            ) {
                Text("Rename")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}