package org.menagerie.puppet_master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun MicrophoneInputScreen() {
    val context = LocalContext.current
    val audioProcessor = remember { AudioProcessor(context) }
    var audioLevel by remember { mutableStateOf(0f) }
    var isRecording by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        VolumeIndicator(
            level = audioLevel,
            orientation = Orientation.Vertical,
            modifier = Modifier.size(100.dp, 200.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            if (isRecording) {
                audioProcessor.stop()
            } else {
                audioProcessor.start { level ->
                    audioLevel = level
                }
            }
            isRecording = !isRecording
        }) {
            Text(text = if (isRecording) "Stop" else "Start")
        }
    }
}