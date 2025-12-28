package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.controls.HotkeySelector
import org.menagerie.puppet_master.controls.VolumeIndicator
import org.menagerie.puppet_master.state_machine.HotKeyNode
import org.menagerie.puppet_master.state_machine.VolumeThresholdNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel

@Composable
fun VolumeThresholdNodeView(
    node: VolumeThresholdNode,
    onThresholdChanged: (Float) -> Unit,
    editorViewModel: NodeEditorViewModel,
    canvasCoordinates: LayoutCoordinates
) {
    var currentLevel by remember { mutableFloatStateOf(0f) } // This would be fed by a real audio stream

    NodeView(
        node = node,
        title = "Volume Threshold",
        editorViewModel = editorViewModel,
        canvasCoordinates = canvasCoordinates
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            VolumeIndicator(
                level = currentLevel,
                threshold = node.threshold,
                onThresholdChange = onThresholdChanged,
                modifier = Modifier.height(30.dp).fillMaxWidth()
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
