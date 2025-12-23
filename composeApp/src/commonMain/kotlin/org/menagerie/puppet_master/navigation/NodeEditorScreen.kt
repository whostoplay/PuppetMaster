package org.menagerie.puppet_master.navigation

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.menagerie.puppet_master.MainViewModel
import org.menagerie.puppet_master.localisation.Strings
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
                var boxCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                val gridSize = 40f

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .onGloballyPositioned { boxCoordinates = it }
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.first()
                                    pointerPosition = change.position

                                    val wasDraggingWire = wireDragInfo != null
                                    val isPointerUp = event.changes.any { !it.pressed }

                                    if (wasDraggingWire && isPointerUp) {
                                        editorViewModel.onWireDragEnd()
                                    }
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

                        // Draw Ghost Wire for wire creation
                        wireDragInfo?.let { dragInfo ->
                            val startPosAbsolute = handlePositions["${dragInfo.fromNodeId}-${dragInfo.fromHandleId}"]
                            if (startPosAbsolute != null) {
                                boxCoordinates?.let {
                                    val startPosLocal = startPosAbsolute - it.localToRoot(Offset.Zero)
                                    drawLine(
                                        color = Color.Yellow,
                                        start = startPosLocal,
                                        end = pointerPosition,
                                        strokeWidth = 3f
                                    )
                                }
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
