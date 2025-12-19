package org.menagerie.puppet_master

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Strings.Keys.ASSIGN_STATE_TITLE
import org.menagerie.puppet_master.Strings.Keys.CANCEL_BUTTON
import org.menagerie.puppet_master.Strings.Keys.CONNECTION_FAILED_TEXT
import org.menagerie.puppet_master.Strings.Keys.CONNECTION_FAILED_TITLE
import org.menagerie.puppet_master.Strings.Keys.OVERWRITE_BUTTON
import org.menagerie.puppet_master.Strings.Keys.OVERWRITE_STATE_TEXT
import org.menagerie.puppet_master.Strings.Keys.OVERWRITE_STATE_TITLE
import org.menagerie.puppet_master.Strings.Keys.TRY_AGAIN_BUTTON
import org.menagerie.puppet_master.Strings.Keys.WORK_OFFLINE_BUTTON

/**
 * A composable that manages the display of various dialogs throughout the application.
 *
 * @param viewModel The [MainViewModel] that holds the state for the dialogs.
 */
@Composable
fun AppDialogs(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val activePuppet by viewModel.activePuppet.collectAsState()

    if (uiState.showStateAssignmentDialog && uiState.selectedThreshold != null) {
        StateAssignmentDialog(
            onDismissRequest = { viewModel.hideStateAssignmentDialog() },
            states = activePuppet?.states.orEmpty(),
            onStateSelected = { state ->
                viewModel.assignStateToThreshold(
                    uiState.selectedThreshold!!,
                    state
                )
            }
        )
    }

    if (uiState.showOverwriteConfirmDialog) {
        OverwriteConfirmDialog(
            onDismissRequest = { viewModel.hideOverwriteConfirmDialog() },
            onConfirm = {
                viewModel.forceCreateNewState()
                viewModel.hideOverwriteConfirmDialog()
            }
        )
    }

    if (uiState.showConnectionErrorDialog) {
        ConnectionErrorDialog(
            onDismissRequest = { viewModel.dismissConnectionErrorDialog() },
            onTryAgain = {
                viewModel.connectAndSync()
                viewModel.dismissConnectionErrorDialog()
            },
            onWorkOffline = {
                viewModel.setOperatingMode(OperatingMode.OFFLINE)
                viewModel.dismissConnectionErrorDialog()
            }
        )
    }
}

/**
 * A dialog that allows the user to assign a [PuppetStateInfo] to a given threshold.
 *
 * @param onDismissRequest Called when the user dismisses the dialog.
 * @param states The list of [PuppetStateInfo]s to choose from.
 * @param onStateSelected Called when the user selects a state.
 */
@Composable
private fun StateAssignmentDialog(
    onDismissRequest: () -> Unit,
    states: List<PuppetStateInfo>,
    onStateSelected: (PuppetStateInfo) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(Strings.getString(ASSIGN_STATE_TITLE)) },
        text = {
            LazyColumn {
                items(states) { state ->
                    Text(
                        text = state.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onStateSelected(state) }
                            .padding(8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(Strings.getString(CANCEL_BUTTON))
            }
        }
    )
}

/**
 * A dialog that asks the user to confirm overwriting an existing state.
 *
 * @param onDismissRequest Called when the user dismisses the dialog.
 * @param onConfirm Called when the user confirms the overwrite action.
 */
@Composable
private fun OverwriteConfirmDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(Strings.getString(OVERWRITE_STATE_TITLE)) },
        text = { Text(Strings.getString(OVERWRITE_STATE_TEXT)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(Strings.getString(OVERWRITE_BUTTON))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(Strings.getString(CANCEL_BUTTON))
            }
        }
    )
}

/**
 * A dialog that informs the user about a server connection failure and provides options to retry or work offline.
 *
 * @param onDismissRequest Called when the user dismisses the dialog.
 * @param onTryAgain Called when the user chooses to try connecting again.
 * @param onWorkOffline Called when the user chooses to work offline.
 */
@Composable
private fun ConnectionErrorDialog(
    onDismissRequest: () -> Unit,
    onTryAgain: () -> Unit,
    onWorkOffline: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(Strings.getString(CONNECTION_FAILED_TITLE)) },
        text = { Text(Strings.getString(CONNECTION_FAILED_TEXT)) },
        confirmButton = {
            TextButton(onClick = onTryAgain) {
                Text(Strings.getString(TRY_AGAIN_BUTTON))
            }
        },
        dismissButton = {
            TextButton(onClick = onWorkOffline) {
                Text(Strings.getString(WORK_OFFLINE_BUTTON))
            }
        }
    )
}
