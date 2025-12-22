package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.Strings
import org.menagerie.puppet_master.state_machine.editor.NodeCanvas
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.state_machine.editor.NodePalette
import kotlin.math.round

data class NodeEditorScreen(private val mainViewModel: MainViewModel) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val editorViewModel: NodeEditorViewModel = rememberScreenModel { NodeEditorViewModel(mainViewModel) }
        val draggedNode by editorViewModel.draggedNode.collectAsState()
        val wireDragInfo by editorViewModel.wireDragInfo.collectAsState()
        val graph by editorViewModel.nodeGraph.collectAsState()

        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(Strings.getString(Strings.Keys.STATE_GRAPH_BUTTON)) },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = Strings.getString(Strings.Keys.BACK_BUTTON_CONTENT_DESCRIPTION))
                        }
                    }
                )
            }
        ) { innerPadding ->
            Row(Modifier.fillMaxSize().padding(innerPadding)) {
                NodePalette(
                    modifier = Modifier.width(250.dp).fillMaxHeight(),
                    editorViewModel = editorViewModel
                )

                var pointerPosition by remember { mutableStateOf(Offset.Zero) }
                val gridSize = 40f

                Box(
                    modifier = Modifier.weight(1f).pointerInput(Unit) {
                        detectTapGestures(
                            onPress = { offset ->
                                pointerPosition = offset
                                val released = try {
                                    tryAwaitRelease()
                                } catch (e: Exception) {
                                    false
                                }

                                if (released) {
                                    draggedNode?.let {
                                        val snappedX = (round(pointerPosition.x / gridSize) * gridSize)
                                        val snappedY = (round(pointerPosition.y / gridSize) * gridSize)
                                        editorViewModel.addNode(it, Offset(snappedX, snappedY))
                                        editorViewModel.onNodeDragEnd()
                                    }
                                }
                            }
                        )
                    }.pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                pointerPosition = event.changes.first().position
                            }
                        }
                    }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Draw Grid
                        for (x in 0..size.width.toInt() step gridSize.toInt()) {
                            drawLine(
                                color = Color.DarkGray,
                                start = Offset(x.toFloat(), 0f),
                                end = Offset(x.toFloat(), size.height),
                                strokeWidth = 1f
                            )
                        }
                        for (y in 0..size.height.toInt() step gridSize.toInt()) {
                            drawLine(
                                color = Color.DarkGray,
                                start = Offset(0f, y.toFloat()),
                                end = Offset(size.width, y.toFloat()),
                                strokeWidth = 1f
                            )
                        }

                        // Draw Ghost Node for node creation
                        draggedNode?.let {
                            val snappedX = (round(pointerPosition.x / gridSize) * gridSize)
                            val snappedY = (round(pointerPosition.y / gridSize) * gridSize)
                            drawRect(
                                color = Color.White.copy(alpha = 0.5f),
                                topLeft = Offset(snappedX, snappedY),
                                size = Size(150f, 100f) // A default size for the ghost
                            )
                        }

                        // Draw Ghost Wire for wire creation
                        wireDragInfo?.let { dragInfo ->
                            val fromNode = graph.nodes[dragInfo.fromNodeId]
                            if (fromNode != null) {
                                // This is a simplification. A real implementation would need to calculate
                                // the exact handle position. For now, let's draw from the node's center.
                                val nodeCenter = fromNode.position + Offset(75f, 50f)
                                drawLine(
                                    color = Color.Yellow,
                                    start = nodeCenter,
                                    end = pointerPosition,
                                    strokeWidth = 3f
                                )
                            }
                        }
                    }
                    NodeCanvas(
                        mainViewModel = mainViewModel, 
                        editorViewModel = editorViewModel
                    )
                }
            }
        }
    }
}
