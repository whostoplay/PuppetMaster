package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.controls.HotkeySelector
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.state_machine.DrawMode
import org.menagerie.puppet_master.state_machine.HotKeyNode
import org.menagerie.puppet_master.state_machine.PhonemeMatchNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.state_machine.editor.viewmodels.PhonemeViewModel

@Composable
fun VolumeThresholdNodeView(
    node: VolumeThresholdNode,
    onThresholdChanged: (Float) -> Unit,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit
) {
    val rawAudioLevel by editorViewModel.mainViewModel.rawAudioLevel.collectAsState()

    NodeView(
        node = node,
        title = "Volume Threshold",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates,
        expanded = expanded
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
            if (expanded) {
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Spike Detection")
                        Switch(
                            checked = node.spikeDetection.enabled,
                            onCheckedChange = { isChecked ->
                                editorViewModel.updateNode(node.copy(spikeDetection = node.spikeDetection.copy(enabled = isChecked)))
                            }
                        )
                    }

                    if (node.spikeDetection.enabled) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Text("Spike Threshold: %.2f".format(node.spikeDetection.threshold))
                            Slider(
                                value = node.spikeDetection.threshold,
                                onValueChange = {
                                    editorViewModel.updateNode(
                                        node.copy(spikeDetection = node.spikeDetection.copy(threshold = it))
                                    )
                                },
                                valueRange = 0.01f..1f
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Column {
                            Text("Spike Window: ${node.spikeDetection.window}")
                            Slider(
                                value = node.spikeDetection.window.toFloat(),
                                onValueChange = {
                                    editorViewModel.updateNode(
                                        node.copy(spikeDetection = node.spikeDetection.copy(window = it.toInt()))
                                    )
                                },
                                valueRange = 2f..100f,
                                steps = 98
                            )
                        }
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    if (expanded) {
                        Icon(Icons.Default.ArrowDropUp, "Collapse")
                    } else {
                        Icon(Icons.Default.ArrowDropDown, "Expand")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
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
    onExpandedChange: (Boolean) -> Unit
) {
    val frequencyData by editorViewModel.mainViewModel.frequencyData.collectAsState()
    var drawMode by remember(node.rule?.conditions?.size) {
        val initialMode = if ((node.rule?.conditions?.size ?: 0) > 1) {
            DrawMode.ADD
        } else {
            DrawMode.REPLACE
        }
        mutableStateOf(initialMode)
    }

    var selectedConditionIndex by remember { mutableStateOf<Int?>(null) }
    val phonemeViewModel: PhonemeViewModel = remember { PhonemeViewModel() }

    LaunchedEffect(node.rule?.conditions?.size) {
        if (selectedConditionIndex != null && selectedConditionIndex!! >= (node.rule?.conditions?.size ?: 0)) {
            selectedConditionIndex = null
        }
    }

    val significantPeaks = remember(frequencyData) {
        node.processFrequencyData(frequencyData)
    }

    val currentPeakCount = significantPeaks.count { (freq, mag) ->
        node.rule?.conditions?.any { condition ->
            condition.type == org.menagerie.puppet_master.state_machine.ConditionType.AND &&
                    freq in condition.frequencyRange && mag in condition.magnitudeRange
        } ?: false
    }

    NodeView(
        node = node,
        title = "Phoneme Match",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates,
        expanded = expanded
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            if (expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    @Composable
                    fun ModeToggleButton(mode: DrawMode, selected: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
                        IconToggleButton(checked = selected, onCheckedChange = { if (it) onClick() }) {
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .padding(4.dp)
                            ) {
                                content()
                            }
                        }
                    }

                    ModeToggleButton(DrawMode.REPLACE, drawMode == DrawMode.REPLACE, { drawMode = DrawMode.REPLACE }) {
                        Icon(Icons.Default.Create, "Replace Rule")
                    }
                    ModeToggleButton(DrawMode.ADD, drawMode == DrawMode.ADD, { drawMode = DrawMode.ADD }) {
                        Icon(Icons.Default.Add, "Add Condition")
                    }
                    ModeToggleButton(DrawMode.SUBTRACT, drawMode == DrawMode.SUBTRACT, { drawMode = DrawMode.SUBTRACT }) {
                        Icon(Icons.Default.Remove, "Subtract Condition (NOT)")
                    }
                }

                FrequencyGraph(
                    modifier = Modifier.fillMaxWidth().height(300.dp),
                    frequencyData = frequencyData,
                    rule = node.rule,
                    onRuleChanged = { newRule ->
                        editorViewModel.updateNode(node.copy(rule = newRule))
                    },
                    onPeaksDetected = { /* No-op, handled by the node now */ },
                    drawMode = drawMode,
                    selectedConditionIndex = selectedConditionIndex,
                    onConditionSelected = { index ->
                        selectedConditionIndex = index
                    },
                    viewModel = phonemeViewModel
                )
                val currentRule = node.rule
                if (selectedConditionIndex != null && currentRule != null) {
                    val selectedIndex = selectedConditionIndex!!
                    val selectedCondition = currentRule.conditions.getOrNull(selectedIndex)
                    if (selectedCondition != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Required Hits:")
                                IconButton(onClick = {
                                    val newHits = (selectedCondition.requiredHits - 1).coerceAtLeast(0)
                                    val newConditions = currentRule.conditions.toMutableList()
                                    newConditions[selectedIndex] = selectedCondition.copy(requiredHits = newHits)
                                    editorViewModel.updateNode(node.copy(rule = currentRule.copy(conditions = newConditions)))
                                }) {
                                    Icon(Icons.Default.Remove, "Decrement Hits")
                                }
                                Text("${selectedCondition.requiredHits}")
                                IconButton(onClick = {
                                    val newHits = selectedCondition.requiredHits + 1
                                    val newConditions = currentRule.conditions.toMutableList()
                                    newConditions[selectedIndex] = selectedCondition.copy(requiredHits = newHits)
                                    editorViewModel.updateNode(node.copy(rule = currentRule.copy(conditions = newConditions)))
                                }) {
                                    Icon(Icons.Default.Add, "Increment Hits")
                                }
                            }

                            Button(
                                onClick = {
                                    val newConditions = currentRule.conditions.toMutableList()
                                    newConditions.removeAt(selectedIndex)
                                    editorViewModel.updateNode(node.copy(rule = currentRule.copy(conditions = newConditions)))
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, "Delete Condition")
                            }
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    Text("Confidence (Consistency) Threshold: ${node.triggerThreshold}")
                    Slider(
                        value = node.triggerThreshold.toFloat(),
                        onValueChange = {
                            editorViewModel.updateNode(node.copy(triggerThreshold = it.toInt()))
                        },
                        valueRange = 1f..20f,
                        steps = 19
                    )
                    Text("Confidence Decay Rate: ${node.confidenceDecayRate}")
                    Slider(
                        value = node.confidenceDecayRate.toFloat(),
                        onValueChange = {
                            editorViewModel.updateNode(node.copy(confidenceDecayRate = it.toInt()))
                        },
                        valueRange = 1f..5f,
                        steps = 4
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    val conditionCount = node.rule?.conditions?.size ?: 0
                    Text("Conditions: $conditionCount", modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Live Hits: $currentPeakCount")
                }

            } else {
                val conditionCount = node.rule?.conditions?.size ?: 0
                val andConditions = node.rule?.conditions?.count { it.type == org.menagerie.puppet_master.state_machine.ConditionType.AND } ?: 0
                val notConditions = node.rule?.conditions?.count { it.type == org.menagerie.puppet_master.state_machine.ConditionType.NOT } ?: 0

                if (conditionCount > 0) {
                    Text("Rule: $conditionCount conditions")
                    Text("  - $andConditions AND boxes")
                    Text("  - $notConditions NOT boxes")
                } else {
                    Text("No rule defined.")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onExpandedChange(!expanded) }) {
                    if (expanded) {
                        Icon(Icons.Default.ArrowDropUp, "Collapse")
                    } else {
                        Icon(Icons.Default.ArrowDropDown, "Expand")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}
