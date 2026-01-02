package org.menagerie.puppet_master.state_machine

import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.Layer
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
    data class SetState(val stateName: String, val puppetId: String? = null, val effect: SpecialEffect? = null, val layers: List<Layer> = emptyList()) : GraphAction
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
        val branchLayers = mutableListOf<Layer>()

        while (currentNode != null) {

            val resumeTime = delayedNodes[currentNode.id]
            if (resumeTime != null) { // Node is in delayedNodes
                if (System.currentTimeMillis() < resumeTime) {
                   return null
                }

                if (currentNode is GoThroughStateNode) {
                    val nextNodeId = currentNode.findNextNodeId(graph, "out")
                    if (nextNodeId != null) {
                        val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                        wire?.let { activeWires.add(it) }
                        activeNodes.add(nextNodeId)
                        currentNode = graph.nodes[nextNodeId]
                        continue // loop to process next node
                    } else {
                       return null
                    }
                }

                delayedNodes.remove(currentNode.id)
            }

            if (currentNode is WithEffectNode) {
                branchEffect = currentNode.effect
                if (currentNode.puppetId != null) {
                    executionContext = executionContext.copy(puppetId = currentNode.puppetId)
                }
                val nextNodeId = currentNode.findNextNodeId(graph, "out")
                if (nextNodeId != null) {
                    val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                    currentNode = graph.nodes[nextNodeId]
                    continue
                } else {
                    break
                }
            }

            if (currentNode is WithLayerNode) {
                branchLayers.add(currentNode.layer)
                if (currentNode.puppetId != null) {
                    executionContext = executionContext.copy(puppetId = currentNode.puppetId)
                }
                val nextNodeId = currentNode.findNextNodeId(graph, "out")
                if (nextNodeId != null) {
                   val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                    currentNode = graph.nodes[nextNodeId]
                    continue
                } else {
                    break
                }
            }

           if (currentNode is GoThroughStateNode) {
                delayedNodes[currentNode.id] = System.currentTimeMillis() + currentNode.delay
                val finalPuppetId = currentNode.puppetId ?: executionContext.puppetId
               return GraphAction.SetState(currentNode.stateName, finalPuppetId, branchEffect, branchLayers)
            }

            if (currentNode is DelayTimerNode) {
                val now = System.currentTimeMillis()
                val triggerTime = delayTimerNodeTimers.getOrPut(currentNode.id) { now + currentNode.delay }

                if (now >= triggerTime) {
                   val nextNodeId = currentNode.findNextNodeId(graph, "out")
                    if (nextNodeId != null) {
                        val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                        wire?.let { activeWires.add(it) }
                        activeNodes.add(nextNodeId)
                        currentNode = graph.nodes[nextNodeId]
                        continue
                    } else {
                        break
                    }
                } else {
                    return null
                }
            }

            if (currentNode is SetPuppetNode) {
               executionContext = executionContext.copy(puppetId = currentNode.puppetId)
            }

            if (currentNode is SetStateNode && currentNode.puppetId == null) {
               currentNode = currentNode.copy(puppetId = executionContext.puppetId)
            }

            if (currentNode is TriggerOnWaitNode) {
                val now = System.currentTimeMillis()
                val triggerTime = waitNodeTimers.getOrPut(currentNode.id) { now + currentNode.waitMillis }

                val result = currentNode.execute(context, graph)
                val nextNodeId = if (now >= triggerTime) {
                   result.nextNodeId
                } else {
                     result.alternativeNextNodeId
                }

                if (nextNodeId != null) {
                   val wire = graph.wires.find { it.fromNodeId == currentNode.id && it.toNodeId == nextNodeId }
                    wire?.let { activeWires.add(it) }
                    activeNodes.add(nextNodeId)
                    currentNode = graph.nodes[nextNodeId]
                    continue
                } else {
                   break
                }
            }

            val result = currentNode.execute(executionContext, graph)


            if (result.action != null) {

                when (val action = result.action) {
                    is GraphAction.SetState -> {
                        println(branchLayers)
                        return action.copy(
                            effect = branchEffect,
                            layers = (action.layers + branchLayers).distinct()
                        )
                    }
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
            val startNodeId = overrideStartNodeId ?: graph.startNodeId
            val startNode = startNodeId?.let { graph.nodes[it] }

            if (startNode == null) {
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
