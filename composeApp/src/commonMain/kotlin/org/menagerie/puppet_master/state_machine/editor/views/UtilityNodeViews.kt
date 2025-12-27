package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.state_machine.DelayTimerNode
import org.menagerie.puppet_master.state_machine.ResetSetNode
import org.menagerie.puppet_master.state_machine.SetPuppetMode
import org.menagerie.puppet_master.state_machine.SetPuppetNode
import org.menagerie.puppet_master.state_machine.TriggerOnWaitNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel

@Composable
fun SetPuppetNodeView(
    node: SetPuppetNode,
    editorViewModel: NodeEditorViewModel,
) {
    NodeView(
        node = node,
        title = "Set Puppet",
        editorViewModel = editorViewModel
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val troupe by editorViewModel.mainViewModel.troupe.collectAsState()
            val puppets = troupe?.puppets ?: emptyList()
            var expanded by remember { mutableStateOf(false) }
            Text("Puppet: ")
            Box {
                Row(
                    modifier = Modifier.clickable { expanded = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(node.puppetId ?: "Select Puppet", color = Color.White)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Puppet", tint = Color.White)
                }
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    puppets.forEach { puppet ->
                        DropdownMenuItem(
                            text = { Text(puppet.name) },
                            onClick = {
                                editorViewModel.updateNode(node.copy(puppetId = puppet.name))
                                expanded = false
                            }
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Switch")
                Switch(
                    checked = node.mode == SetPuppetMode.SET,
                    onCheckedChange = {
                        val newMode = if (it) SetPuppetMode.SET else SetPuppetMode.SWITCH
                        editorViewModel.updateNode(node.copy(mode = newMode))
                    }
                )
                Text("Set")
            }
        }
    }
}

@Composable
fun ResetSetNodeView(
    node: ResetSetNode,
    editorViewModel: NodeEditorViewModel,
) {
    NodeView(
        node = node,
        title = "Reset Start",
        editorViewModel = editorViewModel
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("RESET")
        }
    }
}

@Composable
fun DelayTimerNodeView(
    node: DelayTimerNode,
    editorViewModel: NodeEditorViewModel,
) {
    var sliderPosition by remember { mutableStateOf(node.delay.toFloat()) }

    LaunchedEffect(node.delay) {
        sliderPosition = node.delay.toFloat()
    }

    NodeView(
        node = node,
        title = "Delay Timer",
        editorViewModel = editorViewModel
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Delay: ${sliderPosition.toLong() / 1000f} s", color = Color.White)
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    editorViewModel.updateNode(node.copy(delay = sliderPosition.toLong()))
                },
                valueRange = 0f..10000f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun TriggerOnWaitNodeView(
    node: TriggerOnWaitNode,
    editorViewModel: NodeEditorViewModel,
) {
    var sliderPosition by remember { mutableFloatStateOf(node.waitMillis.toFloat()) }

    LaunchedEffect(node.waitMillis) {
        sliderPosition = node.waitMillis.toFloat()
    }

    NodeView(
        node = node,
        title = "Trigger on Wait",
        editorViewModel = editorViewModel
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Time: ${sliderPosition.toLong() / 1000f} s", color = Color.White)
            Slider(
                value = sliderPosition,
                onValueChange = {
                    sliderPosition = it
                },
                onValueChangeFinished = {
                    editorViewModel.updateNode(node.copy(waitMillis = sliderPosition.toLong()))
                },
                valueRange = 100f..10000f,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
