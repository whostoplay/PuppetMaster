package org.menagerie.puppet_master.state_machine

import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.SpecialEffect

/**
 * A data container that provides the live values that a graph needs to execute its logic.
 * This object is created and updated by the MainViewModel.
 */
data class GraphExecutionContext(
    val microphoneVolume: Float = 0f,
    val hotKeyPressed: Hotkey? = null,
    val toggledOnNodes: Set<NodeId> = emptySet(),
    val puppetId: String? = null,
    val lastProcessedHotkey: Hotkey? = null, // We need to know if a key press has already been handled
    val frequencyPeaks: List<Pair<Float, Float>> = emptyList()
)

/**
 * Represents an action that the UI should take based on the graph's execution.
 */
sealed interface GraphAction {
    data class SetState(val stateName: String, val puppetId: String? = null, val effect: SpecialEffect? = null) : GraphAction
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
    private val waitNodeTimers = mutableMapOf<NodeId, Long>()
    private val delayedNodes = mutableMapOf<NodeId, Long>()
    private val delayTimerNodeTimers = mutableMapOf<NodeId, Long>()

    fun getActiveNodes(): Set<NodeId> = activeNodes.toSet()
    fun getActiveWires(): Set<Wire> = activeWires.toSet()

    /**
     * Executes a branch of the graph starting from a given node.
     * @return An action to be performed, or null if execution completes without action.
     */
    private fun executeFromNode(startNode: Node, context: GraphExecutionContext): GraphAction? {
        var currentNode: Node? = startNode
        var executionContext = context
        var branchEffect: SpecialEffect? = null
        println("--- Executing new branch from ${startNode.id} ---")

        while (currentNode != null) {
            println("Executing node: ${currentNode.id} of type ${currentNode::class.simpleName}")

            val resumeTime = delayedNodes[currentNode.id]
            if (resumeTime != null) { // Node is in delayedNodes
                println("Node ${currentNode.id} is in delayedNodes. Resume time: $resumeTime")
                if (System.currentTimeMillis() < resumeTime) {
                    println("Still waiting for node ${currentNode.id}. Returning null.")
                    // Still waiting, for any kind of delayed node.
                    return null
                }

                println("Delay over for node ${currentNode.id}.")
                // Time is up.
                if (currentNode is GoThroughStateNode) {
                    println("Node ${currentNode.id} is a GoThroughStateNode.")
                    // For GoThroughStateNode, we don't remove it from delayedNodes. We just try to move to the next node.
                    val nextNodeId = currentNode.findNextNodeId(graph, "out")
                    if (nextNodeId != null) {
                        println("Found next node for GoThroughStateNode: $nextNodeId")
                        val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                        wire?.let { activeWires.add(it) }
                        activeNodes.add(nextNodeId)
                        currentNode = graph.nodes[nextNodeId]
                        continue // loop to process next node
                    } else {
                        println("GoThroughStateNode ${currentNode.id} is terminal. Returning null.")
                        // It's a terminal GoThroughStateNode, delay is over.
                        // We do nothing and just stop this branch.
                        // Because it's still in delayedNodes, next tick will also pass through here and stop.
                        return null
                    }
                }

                println("Removing node ${currentNode.id} from delayedNodes.")
                // For other types of delayed nodes (e.g. from RequestDelay), we remove them so they can be re-triggered.
                delayedNodes.remove(currentNode.id)
            }

            if (currentNode is WithEffectNode) {
                println("Node ${currentNode.id} is a WithEffectNode.")
                branchEffect = currentNode.effect
                val nextNodeId = currentNode.findNextNodeId(graph, "out")
                if (nextNodeId != null) {
                    println("Found next node for WithEffectNode: $nextNodeId")
                    val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                    currentNode = graph.nodes[nextNodeId]
                    continue
                } else {
                    println("WithEffectNode ${currentNode.id} is terminal. Breaking loop.")
                    break
                }
            }
            
            // This part is for nodes NOT in delayedNodes, or for nodes whose delay just finished and were removed.
            if (currentNode is GoThroughStateNode) {
                println("Node ${currentNode.id} is a GoThroughStateNode (and not in delayedNodes).")
                // This will only be reached if the node was not in delayedNodes.
                delayedNodes[currentNode.id] = System.currentTimeMillis() + currentNode.delay
                val finalPuppetId = currentNode.puppetId ?: executionContext.puppetId
                println("Returning SetState action for GoThroughStateNode ${currentNode.id}.")
                return GraphAction.SetState(currentNode.stateName, finalPuppetId, branchEffect)
            }

            if (currentNode is DelayTimerNode) {
                println("Node ${currentNode.id} is a DelayTimerNode.")
                val now = System.currentTimeMillis()
                val triggerTime = delayTimerNodeTimers.getOrPut(currentNode.id) { now + currentNode.delay }

                if (now >= triggerTime) {
                    println("DelayTimerNode ${currentNode.id} finished. Continuing to next node.")
                    // Timer is done, continue to the next node.
                    val nextNodeId = currentNode.findNextNodeId(graph, "out")
                    if (nextNodeId != null) {
                        println("Found next node for DelayTimerNode: $nextNodeId")
                        val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                        wire?.let { activeWires.add(it) }
                        activeNodes.add(nextNodeId)
                        currentNode = graph.nodes[nextNodeId]
                        continue
                    } else {
                        println("DelayTimerNode ${currentNode.id} is terminal. Breaking loop.")
                        // No next node, branch ends.
                        break
                    }
                } else {
                    println("DelayTimerNode ${currentNode.id} is still waiting. Returning null.")
                    // Still waiting for the timer to finish. Stop this branch.
                    return null
                }
            }

            if (currentNode is SetPuppetNode) {
                println("Node ${currentNode.id} is a SetPuppetNode. Updating context.")
                executionContext = executionContext.copy(puppetId = currentNode.puppetId)
            }

            if (currentNode is SetStateNode && currentNode.puppetId == null) {
                println("Node ${currentNode.id} is a SetStateNode with null puppetId. Updating puppetId from context.")
                currentNode = currentNode.copy(puppetId = executionContext.puppetId)
            }

            if (currentNode is TriggerOnWaitNode) {
                println("Node ${currentNode.id} is a TriggerOnWaitNode.")
                val now = System.currentTimeMillis()
                val triggerTime = waitNodeTimers.getOrPut(currentNode.id) { now + currentNode.waitMillis }

                println("Executing TriggerOnWaitNode ${currentNode.id} to get result.")
                val result = currentNode.execute(context, graph)
                val nextNodeId = if (now >= triggerTime) {
                    println("Wait time is over. Using nextNodeId: ${result.nextNodeId}")
                    result.nextNodeId
                } else {
                    println("Still waiting. Using alternativeNextNodeId: ${result.alternativeNextNodeId}")
                    result.alternativeNextNodeId
                }

                if (nextNodeId != null) {
                    println("Found next node for TriggerOnWaitNode: $nextNodeId")
                    val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                    currentNode = graph.nodes[nextNodeId]
                    continue
                } else {
                    println("TriggerOnWaitNode ${currentNode.id} is terminal. Breaking loop.")
                    break
                }
            }

            println(">>> Calling execute() on node ${currentNode.id} of type ${currentNode::class.simpleName} <<<")
            val result = currentNode.execute(executionContext, graph)
            println("<<< execute() returned for node ${currentNode.id}. Result has action: ${result.action != null}, nextNodeId: ${result.nextNodeId}")

            if (result.action != null) {
                println("Node ${currentNode.id} returned an action: ${result.action::class.simpleName}")
                when (val action = result.action) {
                    is GraphAction.SetState -> return action.copy(effect = branchEffect)
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
                    }
                    is GraphAction.RequestDelay -> {
                        delayedNodes[action.nextNodeId] = System.currentTimeMillis() + action.delay
                        val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == action.nextNodeId }
                        wire?.let { activeWires.add(it) }
                        activeNodes.add(action.nextNodeId)
                        return null
                    }
                    else -> return action
                }
            }

            if (result.nextNodeId != null) {
                println("Node ${currentNode.id} has nextNodeId: ${result.nextNodeId}. Continuing loop.")
                val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == result.nextNodeId }
                wire?.let { activeWires.add(it) }
                activeNodes.add(result.nextNodeId)
                currentNode = graph.nodes[result.nextNodeId]
            } else {
                println("Node ${currentNode.id} has no nextNodeId. Breaking loop.")
                break
            }
        }
        println("--- Branch execution finished. ---")
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
            val startNodeId = overrideStartNodeId ?: graph.startNodeId
            val startNode = startNodeId?.let { graph.nodes[it] }

            if (startNode == null) {
                if (startNodeId != null) {
                    println("GraphExecutor: Start node with id $startNodeId not found in graph.")
                } else {
                    println("GraphExecutor: No start node defined for the graph.")
                }
                return null
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
                        return tick(context)
                    }

                    if (action is GraphAction.ResetGraphStart) {
                        overrideStartNodeId = null
                        return null
                    }
                    return action
                }

                // If action is null, check if the branch is just paused.
                if (activeNodes.any { delayedNodes.containsKey(it) || delayTimerNodeTimers.containsKey(it) }) {
                    // A higher-priority branch is waiting, so don't process any lower-priority branches.
                    return null
                }
            }

            return null
        } finally {
            val allWaitNodes = graph.nodes.values.filterIsInstance<TriggerOnWaitNode>()
            for (waitNode in allWaitNodes) {
                if (waitNode.id !in activeNodes) {
                    waitNodeTimers.remove(waitNode.id)
                }
            }

            val allDelayTimerNodes = graph.nodes.values.filterIsInstance<DelayTimerNode>()
            for (delayTimerNode in allDelayTimerNodes) {
                if (delayTimerNode.id !in activeNodes) {
                    delayTimerNodeTimers.remove(delayTimerNode.id)
                }
            }
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
        activeNodes.clear()
        activeWires.clear()
        waitNodeTimers.clear()
        delayedNodes.clear()
        delayTimerNodeTimers.clear()
    }
}
