package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.controls.HotkeySelector
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.state_machine.HotKeyNode
import org.menagerie.puppet_master.state_machine.PhonemeMatchNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel

@Composable
fun VolumeThresholdNodeView(
    node: VolumeThresholdNode,
    onThresholdChanged: (Float) -> Unit,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates
) {
    val rawAudioLevel by editorViewModel.mainViewModel.rawAudioLevel.collectAsState()

    NodeView(
        node = node,
        title = "Volume Threshold",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            VolumeIndicator(
                level = rawAudioLevel,
                threshold = node.threshold,
                onThresholdChange = onThresholdChanged,
                sensitivity = node.sensitivity,
                onSensitivityChange = { newSensitivity ->
                    editorViewModel.updateNode(node.copy(sensitivity = newSensitivity))
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HotKeyNodeView(
    node: HotKeyNode,
    onHotKeyChanged: (Hotkey) -> Unit,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates
) {
    NodeView(
        node = node,
        title = "Hot Key",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            HotkeySelector(
                label = "Hotkey",
                hotkey = node.hotkey,
                onHotkeyChanged = onHotKeyChanged,
                splitLevel = true,
            )
        }
    }
}

@Composable
fun PhonemeMatchNodeView(
    node: PhonemeMatchNode,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onPeaksDetected: (List<Pair<Float, Float>>) -> Unit
) {
    val frequencyData by editorViewModel.mainViewModel.frequencyData.collectAsState()
    var currentPeakCount by remember { mutableStateOf(0) }

    NodeView(
        node = node,
        title = "Phoneme Match",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates,
        expanded = expanded
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            if (expanded) {
                FrequencyGraph(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    frequencyData = frequencyData,
                    selectedFrequencyRange = node.frequencyRange,
                    selectedMagnitudeRange = node.magnitudeRange,
                    onAreaSelected = { frequencyRange, magnitudeRange ->
                        editorViewModel.updateNode(
                            node.copy(
                                frequencyRange = frequencyRange,
                                magnitudeRange = magnitudeRange
                            )
                        )
                    },
                    onPeaksDetected = { peaks ->
                        onPeaksDetected(peaks)
                        currentPeakCount = peaks.count { (freq, mag) ->
                            freq in node.frequencyRange && mag in node.magnitudeRange
                        }
                    }
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    Text("Min Peaks: ${node.minPeaks}", modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        editorViewModel.updateNode(node.copy(minPeaks = (node.minPeaks - 1).coerceAtLeast(1)))
                    }) {
                        Icon(Icons.Default.Remove, contentDescription = "Decrement Min Peaks")
                    }
                    IconButton(onClick = {
                        editorViewModel.updateNode(node.copy(minPeaks = node.minPeaks + 1))
                    }) {
                        Icon(Icons.Default.Add, contentDescription = "Increment Min Peaks")
                    }
                    Text("Live Hits: $currentPeakCount")
                }
            } else {
                Text("Frequency: %.0f-%.0f Hz".format(node.frequencyRange.start, node.frequencyRange.endInclusive))
                Text("Magnitude: %.0f-%.0f".format(node.magnitudeRange.start, node.magnitudeRange.endInclusive))
                Text("Min Peaks: ${node.minPeaks}")
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = "Expand")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
