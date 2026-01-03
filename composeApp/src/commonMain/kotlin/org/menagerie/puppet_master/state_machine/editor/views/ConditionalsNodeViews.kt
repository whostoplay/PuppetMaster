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
    var drawMode by remember(node.rule?.conditions?.size) {
        val initialMode = if ((node.rule?.conditions?.size ?: 0) > 1) {
            DrawMode.ADD
        } else {
            DrawMode.REPLACE
        }
        mutableStateOf(initialMode)
    }

    var selectedConditionIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(node.rule?.conditions?.size) {
        if (selectedConditionIndex != null && selectedConditionIndex!! >= (node.rule?.conditions?.size ?: 0)) {
            selectedConditionIndex = null
        }
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
                // Toolbar for drawing modes goes on top for better UX
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // This is a small, self-contained composable for the buttons
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
                    onPeaksDetected = { peaks ->
                        onPeaksDetected(peaks)
                        // UPDATED: Count hits across all conditions in the rule
                        currentPeakCount = peaks.count { (freq, mag) ->
                            node.rule?.conditions?.any { condition ->
                                // Only count hits for positive (AND) conditions
                                condition.type == org.menagerie.puppet_master.state_machine.ConditionType.AND &&
                                        freq in condition.frequencyRange && mag in condition.magnitudeRange
                            } ?: false
                        }
                    },
                    drawMode = drawMode,
                    selectedConditionIndex = selectedConditionIndex,
                    onConditionSelected = { index ->
                        selectedConditionIndex = index
                    }
                )
                val currentRule = node.rule // Create a local, stable variable
                if (selectedConditionIndex != null && currentRule != null) {
                    val selectedIndex = selectedConditionIndex!!
                    val selectedCondition = currentRule.conditions.getOrNull(selectedIndex) // Use currentRule
                    if (selectedCondition != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Required Hits controls
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Required Hits:")
                                IconButton(onClick = {
                                    val newHits = (selectedCondition.requiredHits - 1).coerceAtLeast(0)
                                    val newConditions = currentRule.conditions.toMutableList() // Use currentRule
                                    newConditions[selectedIndex] = selectedCondition.copy(requiredHits = newHits)
                                    editorViewModel.updateNode(node.copy(rule = currentRule.copy(conditions = newConditions))) // Use currentRule
                                }) {
                                    Icon(Icons.Default.Remove, "Decrement Hits")
                                }
                                Text("${selectedCondition.requiredHits}")
                                IconButton(onClick = {
                                    val newHits = selectedCondition.requiredHits + 1
                                    val newConditions = currentRule.conditions.toMutableList() // Use currentRule
                                    newConditions[selectedIndex] = selectedCondition.copy(requiredHits = newHits)
                                    editorViewModel.updateNode(node.copy(rule = currentRule.copy(conditions = newConditions))) // Use currentRule
                                }) {
                                    Icon(Icons.Default.Add, "Increment Hits")
                                }
                            }

                            // Delete button
                            Button(
                                onClick = {
                                    val newConditions = currentRule.conditions.toMutableList() // Use currentRule
                                    newConditions.removeAt(selectedIndex)
                                    editorViewModel.updateNode(node.copy(rule = currentRule.copy(conditions = newConditions))) // Use currentRule
                                    // The LaunchedEffect will handle setting selectedConditionIndex to null
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.Delete, "Delete Condition")
                            }
                        }
                    }
                }

                // UPDATED: Display information relevant to the new rule system
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) {
                    val conditionCount = node.rule?.conditions?.size ?: 0
                    Text("Conditions: $conditionCount", modifier = Modifier.weight(1f))
                    // The old "Min Peaks" UI is removed as it's now part of each RuleCondition
                    Spacer(modifier = Modifier.weight(1f))
                    Text("Live Hits: $currentPeakCount")
                }

            } else {
                // UPDATED: Display a summary of the rule when collapsed
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

            // This expand/collapse button is common to both states
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

