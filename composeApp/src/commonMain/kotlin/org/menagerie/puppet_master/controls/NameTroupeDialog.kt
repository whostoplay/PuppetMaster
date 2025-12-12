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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NameTroupeDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var troupeName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Name Your Troupe") },
        text = {
            Column {
                Text("This is the first puppet in your troupe. Please name your troupe to continue.")
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    value = troupeName,
                    onValueChange = { troupeName = it },
                    label = { Text("Troupe Name") },
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(troupeName) },
                enabled = troupeName.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}