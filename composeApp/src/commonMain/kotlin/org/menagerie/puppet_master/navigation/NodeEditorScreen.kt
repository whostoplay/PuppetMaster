package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.coroutines.launch
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.localisation.Strings
import org.menagerie.puppet_master.state_machine.editor.Arrangement
import org.menagerie.puppet_master.state_machine.editor.ContextMenuWrapper
import org.menagerie.puppet_master.state_machine.editor.NodeCanvas
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel
import org.menagerie.puppet_master.state_machine.editor.NodePalette

data class NodeEditorScreen(private val mainViewModel: MainViewModel) : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val editorViewModel: NodeEditorViewModel = rememberScreenModel { NodeEditorViewModel(mainViewModel) }
        val wireDragInfo by editorViewModel.wireDragInfo.collectAsState()
        val handlePositions by editorViewModel.handlePositions.collectAsState()
        val highlightMode by editorViewModel.highlightMode.collectAsState()
        val isSimulating by editorViewModel.isSimulating.collectAsState()
        val arrangement by editorViewModel.arrangement.collectAsState()
        val focusRequester = remember { FocusRequester() }

        val horizontalScrollState = rememberScrollState()
        val verticalScrollState = rememberScrollState()
        var canvasOffset by remember { mutableStateOf(IntOffset.Zero) }
        var showClearConfirmationDialog by remember { mutableStateOf(false) }

        if (showClearConfirmationDialog) {
            AlertDialog(
                onDismissRequest = { showClearConfirmationDialog = false },
                title = { Text("Clear Graph?") },
                text = { Text("Are you sure you want to delete all nodes and wires? This action cannot be undone.") },
                confirmButton = {
                    Button(onClick = {
                        editorViewModel.clearGraph()
                        showClearConfirmationDialog = false
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    Button(onClick = { showClearConfirmationDialog = false }) {
                        Text("No")
                    }
                }
            )
        }

        LaunchedEffect(Unit) {
            focusRequester.requestFocus()
            launch {
                verticalScrollState.scrollTo(verticalScrollState.maxValue / 2)
            }
        }

        ContextMenuWrapper(
            editorViewModel = editorViewModel,
            horizontalScrollState = horizontalScrollState,
            verticalScrollState = verticalScrollState,
            canvasOffset = canvasOffset
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text(Strings.getString(Strings.Keys.STATE_GRAPH_BUTTON)) },
                        navigationIcon = {
                            IconButton(onClick = { navigator.pop() }) {
                                Icon(
                                    Icons.Default.ArrowBack,
                                    contentDescription = Strings.getString(Strings.Keys.BACK_BUTTON_CONTENT_DESCRIPTION)
                                )
                            }
                        },
                        actions = {
                            IconButton(onClick = { showClearConfirmationDialog = true }) {
                                Icon(Icons.Default.Delete, contentDescription = "Clear Graph")
                            }
                            IconButton(onClick = { editorViewModel.sortNodes() }) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Sort Nodes")
                            }
                            IconButton(onClick = { editorViewModel.cycleArrangement() }) {
                                when (arrangement) {
                                    Arrangement.SHUFFLE -> Icon(Icons.Default.Shuffle, contentDescription = "Shuffle")
                                    Arrangement.UP -> Icon(Icons.Default.ArrowUpward, contentDescription = "Up")
                                    Arrangement.DOWN -> Icon(Icons.Default.ArrowDownward, contentDescription = "Down")
                                }
                            }
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = Strings.getString(Strings.Keys.HIGHLIGHT_ON))
                                Switch(
                                    checked = highlightMode,
                                    onCheckedChange = { editorViewModel.toggleHighlightMode() },
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            IconButton(onClick = { editorViewModel.toggleSimulation() }) {
                                if (isSimulating) {
                                    Icon(Icons.Default.Stop, contentDescription = "Stop Simulation")
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = "Start Simulation")
                                }
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Row(
                    Modifier.fillMaxSize().padding(innerPadding)
                        .focusRequester(focusRequester).focusable()
                        .onKeyEvent { keyEvent ->
                            editorViewModel.onKeyEvent(keyEvent)
                            true
                        }
                ) {
                    NodePalette(
                        modifier = Modifier.width(250.dp).fillMaxHeight(),
                        editorViewModel = editorViewModel
                    )

                    val gridSize = 40f
                    val canvasSize = 30000.dp

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .onGloballyPositioned { canvasOffset = it.positionInWindow().round() }
                            .pointerInput(Unit) {
                                awaitPointerEventScope {
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val change = event.changes.first()

                                        val wasDraggingWire = wireDragInfo != null
                                        val isPointerUp = event.changes.any { !it.pressed }

                                        if (wasDraggingWire && isPointerUp) {
                                            editorViewModel.onWireDragEnd()
                                        }
                                    }
                                }
                            }
                            .horizontalScroll(horizontalScrollState)
                            .verticalScroll(verticalScrollState)
                    ) {
                        Box(Modifier.size(canvasSize)) {
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
                            }
                            NodeCanvas(
                                mainViewModel = mainViewModel,
                                editorViewModel = editorViewModel,
                                highlightMode = highlightMode,
                                isSimulating = isSimulating
                            )
                        }
                    }
                }
            }
        }
    }
}
