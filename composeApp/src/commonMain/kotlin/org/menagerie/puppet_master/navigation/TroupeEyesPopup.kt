package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.menagerie.puppet_master.EyeState
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.Strings
import org.menagerie.puppet_master.Strings.Keys.LEFT_EYE
import org.menagerie.puppet_master.Strings.Keys.LEFT_PUPIL_CONTENT_DESCRIPTION
import org.menagerie.puppet_master.decodeToImageBitmap

/**
 * A data class to hold information about a specific eye state within a troupe.
 *
 * @param puppetName The name of the puppet the eye state belongs to.
 * @param stateName The name of the state within the puppet.
 * @param eyeState The actual eye state information.
 */
data class TroupeEyeInfo(val puppetName: String, val stateName: String, val eyeState: EyeState)

/**
 * A composable that displays a dialog to select an eye set from the current troupe.
 *
 * @param viewModel The MainViewModel to access troupe data.
 * @param onDismissRequest A callback to be invoked when the dialog is dismissed.
 * @param onApply A callback to be invoked when an eye set is selected and the apply button is clicked.
 */
@Composable
fun TroupeEyesPopup(
    viewModel: MainViewModel,
    onDismissRequest: () -> Unit,
    onApply: (EyeState) -> Unit
) {
    val troupe by viewModel.troupe.collectAsState()
    var eyeInfos by remember { mutableStateOf<List<TroupeEyeInfo>>(emptyList()) }
    var selectedEye by remember { mutableStateOf<TroupeEyeInfo?>(null) }

    LaunchedEffect(troupe) {
        val infos = mutableListOf<TroupeEyeInfo>()
        troupe?.puppets?.forEach { puppet ->
            puppet.states.forEach { state ->
                state.eyeState?.let {
                    infos.add(TroupeEyeInfo(puppet.name, state.name, it))
                }
            }
        }
        eyeInfos = infos
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Card {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(Strings.getString(Strings.Keys.SELECT_AN_EYE_SET))
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 100.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(eyeInfos) { info ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .padding(8.dp)
                                .border(2.dp, if (info == selectedEye) Color.Blue else Color.Transparent)
                                .clickable { selectedEye = info }
                        ) {
                            EyePreview(viewModel, info.eyeState)
                            Text("${info.puppetName}: ${info.stateName}", maxLines = 2)
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismissRequest) {
                        Text(Strings.getString(Strings.Keys.CANCEL_BUTTON))
                    }
                    Button(
                        onClick = {
                            selectedEye?.let { onApply(it.eyeState) }
                        },
                        enabled = selectedEye != null
                    ) {
                        Text(Strings.getString(Strings.Keys.APPLY))
                    }
                }
            }
        }
    }
}

/**
 * A composable that displays a preview of an eye set.
 *
 * @param viewModel The MainViewModel to fetch image data.
 * @param eyeState The EyeState to be previewed.
 */
@Composable
fun EyePreview(viewModel: MainViewModel, eyeState: EyeState) {
    var leftEyeOpenData by remember { mutableStateOf<ByteArray?>(null) }
    var leftEyePupilData by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(eyeState) {
        coroutineScope {
            val lOpenData = async { viewModel.getImageData(eyeState.eyes.left.openState) }
            val lPupilData = async { eyeState.eyes.left.pupil?.let { viewModel.getImageData(it) } }
            leftEyeOpenData = lOpenData.await()
            leftEyePupilData = lPupilData.await()
        }
    }

    Box(modifier = Modifier.size(50.dp)) {
        leftEyeOpenData?.let { decodeToImageBitmap(it) }?.let {
            Image(bitmap = it, contentDescription = Strings.getString(LEFT_EYE))
        }
        leftEyePupilData?.let { decodeToImageBitmap(it) }?.let {
            Image(bitmap = it, contentDescription = Strings.getString(LEFT_PUPIL_CONTENT_DESCRIPTION))
        }
    }
}
