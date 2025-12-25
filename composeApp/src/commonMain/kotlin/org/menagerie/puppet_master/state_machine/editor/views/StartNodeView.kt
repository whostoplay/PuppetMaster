package org.menagerie.puppet_master.state_machine.editor.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.menagerie.puppet_master.state_machine.StartNode
import org.menagerie.puppet_master.state_machine.editor.NodeEditorViewModel

@Composable
fun StartNodeView(
    node: StartNode,
    editorViewModel: NodeEditorViewModel,
) {
    NodeView(
        node = node,
        title = "Start Node",
        editorViewModel = editorViewModel
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text("START")
        }
    }
}
