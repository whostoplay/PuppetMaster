package org.menagerie.puppet_master.state_machine

import org.menagerie.puppet_master.Hotkey

/**
 * A data container that provides the live values that a graph needs to execute its logic.
 * This object is created and updated by the MainViewModel.
 */
data class GraphExecutionContext(
    val microphoneVolume: Float = 0f,
    val hotKeyPressed: Hotkey? = null,
    val toggledOnNodes: Set<NodeId> = emptySet(),
    val puppetId: String? = null
)

/**
 * Represents an action that the UI should take based on the graph's execution.
 */
sealed interface GraphAction {
    data class SetState(val stateName: String, val puppetId: String? = null) : GraphAction
    data class SetGraphStart(val nodeId: NodeId) : GraphAction
    object ResetGraphStart : GraphAction
}

/**
 * Executes the logic of a NodeGraph based on a given ExecutionContext.
 * This executor is stateful and tracks toggled nodes.
 */
class GraphExecutor(private val graph: NodeGraph) {

    private val toggledOnNodes = mutableSetOf<NodeId>()
    private var lastProcessedHotkey: Hotkey? = null
    private var overrideStartNodeId: NodeId? = null

    /**
     * Executes the graph's logic, starting from the graph's start node.
     * It checks for global "ANY STATE" transitions first, then proceeds with normal
     * graph traversal until an action is returned or a branch ends.
     * @param context The live data from the ViewModel.
     * @return An action to be performed, or null if no action is required.
     */
    fun tick(context: GraphExecutionContext): GraphAction? {
        // Update toggled nodes based on new hotkey presses
        if (context.hotKeyPressed != null && !context.hotKeyPressed.shallowEquals(lastProcessedHotkey)) {
            graph.nodes.values.forEach { node ->
                if (node is HotKeyNode && !node.mode && node.hotkey.shallowEquals(context.hotKeyPressed)) {
                    if (toggledOnNodes.contains(node.id)) {
                        toggledOnNodes.remove(node.id)
                    } else {
                        toggledOnNodes.add(node.id)
                    }
                }
            }
        }
        lastProcessedHotkey = context.hotKeyPressed

        val startNodeId = overrideStartNodeId ?: graph.startNodeId
        val startNode = startNodeId?.let { graph.nodes[it] }

        if (startNode == null) {
            if (startNodeId != null) {
                println("GraphExecutor: Start node with id $startNodeId not found in graph.")
            } else {
                println("GraphExecutor: No start node defined for the graph.")
            }
            return null // Can't execute without a starting node.
        }

        val childrenOfStart = graph.wires
            .filter { it.fromNodeId == startNode.id }
            .mapNotNull { wire -> graph.nodes[wire.toNodeId] }
            .sortedBy { it.branchPriority }

        var executionContext = context.copy(toggledOnNodes = toggledOnNodes)
        if (startNode is StartNode) {
            executionContext = executionContext.copy(puppetId = startNode.puppetId)
        }

        for (childNode in childrenOfStart) {
            var currentNode: Node? = childNode
            if (currentNode == null) continue

            while (currentNode != null) {
                if (currentNode is SetPuppetNode) {
                    executionContext = executionContext.copy(puppetId = currentNode.puppetId)
                }

                if (currentNode is SetStateNode && currentNode.puppetId == null) {
                    currentNode = currentNode.copy(puppetId = executionContext.puppetId)
                }

                val result = currentNode.execute(executionContext, graph)

                if (result.action != null) {
                    when (result.action) {
                        is GraphAction.SetGraphStart -> {
                            overrideStartNodeId = result.action.nodeId
                            // Continue execution from the new start node
                            return tick(context)
                        }
                        is GraphAction.ResetGraphStart -> {
                            overrideStartNodeId = null
                            // Stop execution for this tick
                            return null
                        }
                        else -> return result.action
                    }
                }

                if (result.nextNodeId != null) {
                    currentNode = graph.nodes[result.nextNodeId]
                } else {
                    break
                }
            }
        }

        return null
    }

    /**
     * Resets the executor, clearing all toggled nodes.
     */
    fun reset() {
        toggledOnNodes.clear()
        lastProcessedHotkey = null
        overrideStartNodeId = null
    }
}
