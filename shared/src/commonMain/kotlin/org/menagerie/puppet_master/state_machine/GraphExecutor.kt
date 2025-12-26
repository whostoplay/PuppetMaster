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
    val puppetId: String? = null,
    val lastProcessedHotkey: Hotkey? = null // We need to know if a key press has already been handled
)

/**
 * Represents an action that the UI should take based on the graph's execution.
 */
sealed interface GraphAction {
    data class SetState(val stateName: String, val puppetId: String? = null) : GraphAction
    data class SetGraphStart(val nodeId: NodeId) : GraphAction
    object ResetGraphStart : GraphAction
    // This is an internal action for the executor to handle, requested by a node
    data class RequestToggle(val nodeId: NodeId) : GraphAction
    data class RequestDelay(val nextNodeId: NodeId, val delay: Long) : GraphAction
}

/**
 * Executes the logic of a NodeGraph based on a given ExecutionContext.
 * This executor is stateful and tracks toggled nodes and delayed graph continuations.
 */
class GraphExecutor(private val graph: NodeGraph) {

    private val toggledOnNodes = mutableSetOf<NodeId>()
    private var lastProcessedHotkey: Hotkey? = null
    private var overrideStartNodeId: NodeId? = null

    private data class DelayedContinuation(
        val nodeId: NodeId,
        val context: GraphExecutionContext,
        val resumeTime: Long
    )
    private val pendingContinuations = mutableListOf<DelayedContinuation>()

    /**
     * Executes a branch of the graph starting from a given node.
     * @return An action to be performed, or null if execution completes without action.
     */
    private fun executeFromNode(startNode: Node, context: GraphExecutionContext): GraphAction? {
        var currentNode: Node? = startNode
        var executionContext = context

        while (currentNode != null) {
            if (currentNode is SetPuppetNode) {
                executionContext = executionContext.copy(puppetId = currentNode.puppetId)
            }

            if (currentNode is SetStateNode && currentNode.puppetId == null) {
                currentNode = currentNode.copy(puppetId = executionContext.puppetId)
            }

            if (currentNode is GoThroughStateNode) {
                if (currentNode.puppetId == null) {
                    currentNode = currentNode.copy(puppetId = executionContext.puppetId)
                }
                val result = currentNode.execute(executionContext, graph)
                if (result.nextNodeId != null) {
                    pendingContinuations.add(
                        DelayedContinuation(
                            nodeId = result.nextNodeId,
                            context = executionContext,
                            resumeTime = System.currentTimeMillis() + currentNode.delay
                        )
                    )
                }
                // Always return the action from a GoThroughStateNode and halt this execution path.
                return result.action
            }

            val result = currentNode.execute(executionContext, graph)

            if (result.action != null) {
                when (val action = result.action) {
                    is GraphAction.SetGraphStart -> {
                        overrideStartNodeId = action.nodeId
                        return action
                    }
                    is GraphAction.ResetGraphStart -> {
                        overrideStartNodeId = null
                        return action
                    }
                    is GraphAction.RequestToggle -> {
                        if (toggledOnNodes.contains(action.nodeId)) {
                            toggledOnNodes.remove(action.nodeId)
                        } else {
                            toggledOnNodes.add(action.nodeId)
                        }
                        lastProcessedHotkey = context.hotKeyPressed
                        // Execution continues
                    }
                    is GraphAction.RequestDelay -> {
                        pendingContinuations.add(
                            DelayedContinuation(
                                nodeId = action.nextNodeId,
                                context = executionContext,
                                resumeTime = System.currentTimeMillis() + action.delay
                            )
                        )
                        // Stop execution for this tick
                        return null
                    }
                    else -> return action // This bubbles up to the UI
                }
            }

            if (result.nextNodeId != null) {
                currentNode = graph.nodes[result.nextNodeId]
            } else {
                break
            }
        }
        return null
    }

    /**
     * Executes the graph's logic, starting from the graph's start node.
     * It checks for global "ANY STATE" transitions first, then proceeds with normal
     * graph traversal until an action is returned or a branch ends.
     * @param context The live data from the ViewModel.
     * @return An action to be performed, or null if no action is required.
     */
    fun tick(context: GraphExecutionContext): GraphAction? {
        // Process pending continuations
        val now = System.currentTimeMillis()
        val readyContinuations = pendingContinuations.filter { it.resumeTime <= now }
        if (readyContinuations.isNotEmpty()) {
            pendingContinuations.removeAll(readyContinuations)
            for (continuation in readyContinuations) {
                graph.nodes[continuation.nodeId]?.let { node ->
                    val action = executeFromNode(node, continuation.context)
                    if (action != null) {
                        if (action is GraphAction.SetGraphStart) {
                            overrideStartNodeId = action.nodeId
                            return tick(context) // Restart tick
                        }
                        if (action is GraphAction.ResetGraphStart) {
                            overrideStartNodeId = null
                        }
                        return action
                    }
                }
            }
        }

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

        var executionContext = context.copy(
            toggledOnNodes = toggledOnNodes,
            lastProcessedHotkey = lastProcessedHotkey
        )
        if (startNode is StartNode) {
            executionContext = executionContext.copy(puppetId = startNode.puppetId)
        }

        for (childNode in childrenOfStart) {
            val action = executeFromNode(childNode, executionContext)
            if (action != null) {
                if (action is GraphAction.SetGraphStart) {
                    overrideStartNodeId = action.nodeId
                    return tick(context) // Restart tick
                }
                if (action is GraphAction.ResetGraphStart) {
                    overrideStartNodeId = null
                    return null // Stop execution for this tick
                }
                return action
            }
        }

        // Update the hotkey at the very end of the tick.
        lastProcessedHotkey = context.hotKeyPressed
        return null
    }

    /**
     * Resets the executor, clearing all toggled nodes.
     */
    fun reset() {
        toggledOnNodes.clear()
        lastProcessedHotkey = null
        overrideStartNodeId = null
        pendingContinuations.clear()
    }
}