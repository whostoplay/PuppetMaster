package org.menagerie.puppet_master.state_machine.editor

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.state_machine.ConditionalNode
import org.menagerie.puppet_master.state_machine.SetStateNode
import org.menagerie.puppet_master.state_machine.SetStateNodeView
import org.menagerie.puppet_master.state_machine.StateNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNodeView
import kotlin.math.roundToInt

@Composable
fun NodeCanvas(
    modifier: Modifier = Modifier,
    mainViewModel: MainViewModel,
    editorViewModel: NodeEditorViewModel
) {
    val graph by editorViewModel.nodeGraph.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            graph.wires.forEach { wire ->
                val fromNode = graph.nodes[wire.fromNodeId]
                val toNode = graph.nodes[wire.toNodeId]
                if (fromNode != null && toNode != null) {
                    drawLine(
                        color = Color.Gray,
                        start = fromNode.position,
                        end = toNode.position,
                        strokeWidth = 2f
                    )
                }
            }
        }

        graph.nodes.values.forEach { node ->
            Box(
                modifier = Modifier.offset {
                    IntOffset(node.position.x.roundToInt(), node.position.y.roundToInt())
                }
            ) {
                when (node) {
                    is StateNode -> RenderStateNode(node, mainViewModel, editorViewModel)
                    is ConditionalNode -> RenderConditionalNode(node, mainViewModel, editorViewModel)
                }
            }
        }
    }
}

@Composable
private fun RenderStateNode(node: StateNode, mainViewModel: MainViewModel, editorViewModel: NodeEditorViewModel) {
    val graph by editorViewModel.nodeGraph.collectAsState()
    val puppetStates by mainViewModel.puppetStates.collectAsState()
    val idleImage by mainViewModel.idleImage.collectAsState()

    if (node is SetStateNode) {
        val stateInfo = puppetStates.find { it.name == node.stateName }
        idleImage?.let {
            SetStateNodeView(
                node = node,
                isStartNode = node.id == graph.startNodeId,
                puppetState = stateInfo,
                idleImage = it,
                editorViewModel = editorViewModel,
                uploadsDir = mainViewModel.uploadsDir
            )
        }
    }
}

@Composable
private fun RenderConditionalNode(node: ConditionalNode, mainViewModel: MainViewModel, editorViewModel: NodeEditorViewModel) {
    when (node) {
        is VolumeThresholdNode -> {
            VolumeThresholdNodeView(
                node = node,
                onThresholdChanged = { newThreshold ->
                    editorViewModel.updateNode(node.copy(threshold = newThreshold))
                },
                editorViewModel = editorViewModel
            )
        }
    }
}
