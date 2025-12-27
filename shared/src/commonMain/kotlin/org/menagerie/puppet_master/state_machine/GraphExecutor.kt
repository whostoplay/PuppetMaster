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

    private val activeNodes = mutableSetOf<NodeId>()
    private val activeWires = mutableSetOf<Wire>()
    private val waitNodeCounters = mutableMapOf<NodeId, Int>()

    fun getActiveNodes(): Set<NodeId> = activeNodes.toSet()
    fun getActiveWires(): Set<Wire> = activeWires.toSet()

    private data class DelayedContinuation(
        val nodeId: NodeId,
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
                val finalPuppetId = currentNode.puppetId ?: executionContext.puppetId
                val nextNodeId = currentNode.findNextNodeId(graph, "out")

                if (nextNodeId != null) {
                    pendingContinuations.add(
                        DelayedContinuation(
                            nodeId = nextNodeId,
                            resumeTime = System.currentTimeMillis() + currentNode.delay
                        )
                    )
                    val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                }
                return GraphAction.SetState(currentNode.stateName, finalPuppetId)
            }

            if (currentNode is TriggerOnWaitNode) {
                val thresholdTicks = (currentNode.waitMillis / 16)
                val counter = waitNodeCounters.getOrDefault(currentNode.id, 0) + 1

                val result = currentNode.execute(context, graph) // Now we can rely on the node's own logic
                val nextNodeId = if (counter >= thresholdTicks) {
                    waitNodeCounters.remove(currentNode.id) // Reset counter
                    result.nextNodeId // This will be the "trigger" path
                } else {
                    waitNodeCounters[currentNode.id] = counter
                    result.alternativeNextNodeId // This will be the "fail" path
                }

                if (nextNodeId != null) {
                    val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                    currentNode = graph.nodes[nextNodeId]
                    continue // Continue to the next node in the loop
                } else {
                    break // End of branch
                }
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
                        // Execution continues
                    }
                    is GraphAction.RequestDelay -> {
                        pendingContinuations.add(
                            DelayedContinuation(
                                nodeId = action.nextNodeId,
                                resumeTime = System.currentTimeMillis() + action.delay
                            )
                        )
                        val wire = graph.wires.find { it.fromNodeId == currentNode!!.id && it.toNodeId == action.nextNodeId }
                        wire?.let { activeWires.add(it) }
                        activeNodes.add(action.nextNodeId)
                        // Stop execution for this tick
                        return null
                    }
                    else -> return action // This bubbles up to the UI
                }
            }

            if (result.nextNodeId != null) {
                val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == result.nextNodeId }
                wire?.let { activeWires.add(it) }
                activeNodes.add(result.nextNodeId)
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
        activeNodes.clear()
        activeWires.clear()

        try {
            // Step 1: Process pending continuations. If any are ready, we process them and DO NOT continue to normal execution.
            val now = System.currentTimeMillis()
            val readyContinuations = pendingContinuations.filter { it.resumeTime <= now }

            if (readyContinuations.isNotEmpty()) {
                pendingContinuations.removeAll(readyContinuations)
                var resultingAction: GraphAction? = null
                val continuationContext = context.copy(
                    toggledOnNodes = toggledOnNodes,
                    lastProcessedHotkey = lastProcessedHotkey
                )
                for (continuation in readyContinuations) {
                    graph.nodes[continuation.nodeId]?.let { node ->
                        activeNodes.add(node.id)
                        val action = executeFromNode(node, continuationContext)
                        if (action != null) {
                            resultingAction = action
                        }
                    }
                }

                if (resultingAction is GraphAction.SetGraphStart) {
                    overrideStartNodeId = resultingAction.nodeId
                    return tick(context) // Restart tick immediately
                }
                if (resultingAction is GraphAction.ResetGraphStart) {
                    overrideStartNodeId = null
                }

                return resultingAction // End the tick here.
            }

            // Step 2: Normal execution from start node (only if no continuations were ready)
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

            activeNodes.add(startNode.id)

            val childrenOfStart = graph.wires
                .filter { it.fromNodeId == startNode.id }
                .mapNotNull { wire -> graph.nodes[wire.toNodeId]?.let { node -> wire to node } }
                .sortedBy { it.second.branchPriority }

            var executionContext = context.copy(
                toggledOnNodes = toggledOnNodes,
                lastProcessedHotkey = lastProcessedHotkey
            )
            if (startNode is StartNode) {
                executionContext = executionContext.copy(puppetId = startNode.puppetId)
            }

            for ((wire, childNode) in childrenOfStart) {
                activeWires.add(wire)
                activeNodes.add(childNode.id)
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

            return null
        } finally {
            // Reset counters for any TriggerOnWaitNodes that were not visited this tick
            val allWaitNodes = graph.nodes.values.filterIsInstance<TriggerOnWaitNode>()
            for (waitNode in allWaitNodes) {
                if (waitNode.id !in activeNodes) {
                    waitNodeCounters.remove(waitNode.id)
                }
            }

            // Update the hotkey at the very end of the tick.
            lastProcessedHotkey = context.hotKeyPressed
        }
    }

    /**
     * Resets the executor, clearing all toggled nodes.
     */
    fun reset() {
        toggledOnNodes.clear()
        lastProcessedHotkey = null
        overrideStartNodeId = null
        pendingContinuations.clear()
        activeNodes.clear()
        activeWires.clear()
        waitNodeCounters.clear()
    }
}