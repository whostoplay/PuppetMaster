package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.state_machine.Node
import org.menagerie.puppet_master.state_machine.getAvailableBehaviouralNodes
import org.menagerie.puppet_master.state_machine.getAvailableConditionalNodes
import org.menagerie.puppet_master.state_machine.getAvailableNodes
import org.menagerie.puppet_master.state_machine.getAvailableUtilityNodes

@Composable
fun NodePalette(modifier: Modifier = Modifier, editorViewModel: NodeEditorViewModel) {
    val stateNodes = getAvailableNodes()
    val conditionalNodes = getAvailableConditionalNodes()
    val utilityNodes = getAvailableUtilityNodes()
    val behaviouralNodes = getAvailableBehaviouralNodes()

    LazyColumn(modifier = modifier) {
        item {
            CollapsibleSection("State Nodes") {
                stateNodes.forEach { node ->
                    PaletteItem(node, editorViewModel)
                }
            }
        }
        item {
            CollapsibleSection("Conditional Nodes") {
                conditionalNodes.forEach { node ->
                    PaletteItem(node, editorViewModel)
                }
            }
        }
        item {
            CollapsibleSection("Utility Nodes") {
                utilityNodes.forEach { node ->
                    PaletteItem(node, editorViewModel)
                }
            }
        }
        item {
            CollapsibleSection("Behavioural Nodes") {
                behaviouralNodes.forEach { node ->
                    PaletteItem(node, editorViewModel)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PaletteItem(node: Node, editorViewModel: NodeEditorViewModel) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .pointerInput(node) { // Use node as the key to ensure the correct one is dragged
                detectTapGestures(
                    onDoubleTap = {
                        editorViewModel.addNode(node)
                    }
                )
            }
    ) {
        Text(node::class.simpleName ?: "")
    }
}

@Composable
private fun CollapsibleSection(title: String, content: @Composable () -> Unit) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.graphicsLayer(rotationZ = if (isExpanded) 0f else -90f)
            )
        }

        if (isExpanded) {
            Column(modifier = Modifier.padding(start = 16.dp)) {
                content()
            }
        }
    }
}
