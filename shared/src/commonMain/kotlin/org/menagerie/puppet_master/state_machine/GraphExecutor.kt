package org.menagerie.puppet_master.state_machine

import org.menagerie.puppet_master.Hotkey
import org.menagerie.puppet_master.Layer
import org.menagerie.puppet_master.SpecialEffect


/**
 * Represents the live data context for a single execution of the graph.
 */
data class GraphExecutionContext(
    val microphoneVolume: Float = 0.0f,
    val hotKeyPressed: Hotkey? = null,
    val toggledOnNodes: Set<NodeId> = emptySet(),
    val puppetId: String? = null,
    val lastProcessedHotkey: Hotkey? = null, // We need to know if a key press has already been handled
    val frequencyData: FloatArray = FloatArray(0),
    val phonemeMatchStates: Map<NodeId, PhonemeMatchState> = emptyMap(),
    val volumeHistory: Map<NodeId, List<Float>> = emptyMap()
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
    data class UpdatePhonemeMatchState(val nodeId: NodeId, val state: PhonemeMatchState) : GraphAction
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
    private val randomNodeOrders = mutableMapOf<NodeId, Pair<List<NodeId>, Long>>()
    private val phonemeMatchStates = mutableMapOf<NodeId, PhonemeMatchState>()
    private val volumeHistory = mutableMapOf<NodeId, MutableList<Float>>()

    fun getActiveNodes(): Set<NodeId> = activeNodes.toSet()
    fun getActiveWires(): Set<Wire> = activeWires.toSet()

    /**
     * Executes a branch of the graph starting from a given node.
     * @return An action to be performed, or null if execution completes without action.
     */
    private fun executeFromNode(startNode: Node, context: GraphExecutionContext): GraphAction? {
        // This is the main recursive processing function
        return processNode(startNode, context, null, emptyList())
    }

    private fun processNode(
        currentNode: Node,
        context: GraphExecutionContext,
        inheritedEffect: SpecialEffect?,    inheritedLayers: List<Layer>
    ): GraphAction? {
        // --- 1. Pre-Execution & Context Setup (No changes here, this part is correct) ---
        val resumeTime = delayedNodes[currentNode.id]
        if (resumeTime != null) {
            if (System.currentTimeMillis() < resumeTime) {
                activeNodes.add(currentNode.id)
                return null
            }
            delayedNodes.remove(currentNode.id)
        }
        if (currentNode is DelayTimerNode) {
            val now = System.currentTimeMillis()
            val triggerTime = delayTimerNodeTimers.getOrPut(currentNode.id) { now + currentNode.delay }
            if (now < triggerTime) {
                activeNodes.add(currentNode.id)
                return null
            }
            delayTimerNodeTimers.remove(currentNode.id)
        }

        // --- Special handling for RandomNode (No changes here, this part is correct) ---
        if (currentNode is RandomNode) {
            activeNodes.add(currentNode.id)
            val childrenOfRandom = graph.wires
                .filter { it.fromNodeId == currentNode.id }
                .mapNotNull { wire -> graph.nodes[wire.toNodeId]?.let { node -> wire to node } }
            val now = System.currentTimeMillis()
            val (shuffledIds, expiry) = randomNodeOrders[currentNode.id] ?: (null to 0L)
            val orderedChildren = if (shuffledIds != null && now < expiry) {
                val childrenMap = childrenOfRandom.associateBy { (_, node) -> node.id }
                shuffledIds.mapNotNull { id -> childrenMap[id] }
            } else {
                val shuffled = childrenOfRandom.shuffled()
                val newOrder = shuffled.map { it.second.id }
                randomNodeOrders[currentNode.id] = newOrder to now + currentNode.retainOrderDelay
                shuffled
            }
            for ((wire, childNode) in orderedChildren) {
                activeWires.add(wire)
                val action = processNode(childNode, context, inheritedEffect, inheritedLayers)
                if (action != null) return action
                if (activeNodes.any { delayedNodes.containsKey(it) || delayTimerNodeTimers.containsKey(it) }) return null
            }
            return null
        }

        // --- 2. Execute the current node's specific logic ---
        var executionContext = context
        if (currentNode is SetPuppetNode) {
            executionContext = executionContext.copy(puppetId = currentNode.puppetId)
        }
        val result = currentNode.execute(executionContext, graph)

        // If the node's condition fails (e.g., Hotkey not pressed), stop this path.
        // StartNode is a special case that always "succeeds" without a specific next node.
        if (result.nextNodeId == null && result.action == null && currentNode !is StartNode) {
            return null // This path is dead.
        }

        // --- 3. If execution succeeded, mark active and process results ---
        activeNodes.add(currentNode.id)

        // Handle context-modifying nodes
        var currentEffect = inheritedEffect
        var currentLayers = inheritedLayers.toMutableList()
        if (currentNode is WithEffectNode) {
            currentEffect = currentNode.effect
            if (currentNode.puppetId != null) executionContext = executionContext.copy(puppetId = currentNode.puppetId)
        }
        if (currentNode is WithLayerNode) {
            currentLayers.add(currentNode.layer)
            if (currentNode.puppetId != null) executionContext = executionContext.copy(puppetId = currentNode.puppetId)
        }

        // Handle terminal nodes or nodes that return an immediate action
        if (currentNode is GoThroughStateNode) {
            delayedNodes[currentNode.id] = System.currentTimeMillis() + currentNode.delay
            val finalPuppetId = currentNode.puppetId ?: executionContext.puppetId
            return GraphAction.SetState(currentNode.stateName, finalPuppetId, currentEffect, currentLayers)
        }
        if (result.action != null) {
            return handleNodeAction(result.action, currentNode.id, currentEffect, currentLayers)
        }

        // --- 4. THE CRITICAL FIX: Find and recursively process children ---
        val children = graph.wires
            .filter { it.fromNodeId == currentNode.id }
            .mapNotNull { wire -> graph.nodes[wire.toNodeId]?.let { node -> wire to node } }
            .sortedBy { (_, node) -> node.branchPriority } // Correctly sort by node priority

        // A node is a "pass-through" if its job is to activate its children, not to select one.
        // StartNode and HotkeyNode are primary examples.
        val isPassThroughNode = currentNode is StartNode || currentNode is ConditionalNode || currentNode is BehaviouralNode

        // Iterate through all prioritized children
        for ((wire, childNode) in children) {
            // We process the child if the current node is a pass-through OR
            // if the current node specifically pointed to this child.
            val shouldProcessChild = isPassThroughNode || (result.nextNodeId == childNode.id)

            if (shouldProcessChild) {
                activeWires.add(wire)
                val action = processNode(childNode, executionContext, currentEffect, currentLayers)
                // If any child branch returns a valid action, we stop and return it up the chain.
                if (action != null) return action
                // If a child branch triggered a delay, we must also stop processing siblings.
                if (activeNodes.any { delayedNodes.containsKey(it) || delayTimerNodeTimers.containsKey(it) }) return null
            }
        }

        return null // This path and all its children are dead ends.
    }



    // Helper function to process actions returned by nodes
    private fun handleNodeAction(action: GraphAction, nodeId: NodeId, effect: SpecialEffect?, layers: List<Layer>): GraphAction? {
        when (action) {
            is GraphAction.SetState -> {
                return action.copy(
                    effect = effect,
                    layers = (action.layers + layers).distinct()
                )
            }
            is GraphAction.RequestDelay -> {
                delayedNodes[action.nextNodeId] = System.currentTimeMillis() + action.delay
                val wire = graph.wires.find { it.fromNodeId == nodeId && it.toNodeId == action.nextNodeId }
                wire?.let { activeWires.add(it) }
                activeNodes.add(action.nextNodeId)
                return null // Null signifies we are waiting, not a final action
            }
            is GraphAction.RequestToggle -> {
                if (toggledOnNodes.contains(action.nodeId)) toggledOnNodes.remove(action.nodeId)
                else toggledOnNodes.add(action.nodeId)
            }
            is GraphAction.UpdatePhonemeMatchState -> phonemeMatchStates[action.nodeId] = action.state
            is GraphAction.SetGraphStart -> overrideStartNodeId = action.nodeId
            is GraphAction.ResetGraphStart -> overrideStartNodeId = null
        }
        // Some actions don't terminate the graph walk (like Toggle), others might.
        // For now, we assume most internal actions don't produce a final result for 'tick'.
        return if (action is GraphAction.SetState) action else null
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

        // Update volume history for all nodes that require it
        graph.nodes.values.forEach { node ->
            val historyLimit = when (node) {
                is VolumeThresholdNode -> (node.spikeDetection.window).coerceAtLeast(100)
                is RhythmNode -> (node.beatDetection.memoryFrames).coerceAtLeast(100)
                else -> 0
            }

            if (historyLimit > 0) {
                val history = volumeHistory.getOrPut(node.id) { mutableListOf() }
                history.add(context.microphoneVolume)
                if (history.size > historyLimit) {
                    history.removeAt(0)
                }
            }
        }

        try {
            val startNodeId = overrideStartNodeId ?: graph.startNodeId
            val startNode = startNodeId?.let { graph.nodes[it] } ?: return null

            val executionContext = context.copy(
                toggledOnNodes = toggledOnNodes,
                lastProcessedHotkey = lastProcessedHotkey,
                phonemeMatchStates = phonemeMatchStates,
                volumeHistory = volumeHistory
            )

            // --- UNIFIED EXECUTION START ---
            // We call processNode and let it handle ALL traversal.
            // There is no special logic for the StartNode's children here.
            val action = processNode(startNode, executionContext, null, emptyList())

            // Handle meta-actions that might have been returned from the graph
            if (action is GraphAction.SetGraphStart) {
                overrideStartNodeId = action.nodeId
                // We can re-tick immediately to use the new start node in the same frame
                return tick(context)
            }

            if (action is GraphAction.ResetGraphStart) {
                overrideStartNodeId = null
                return null
            }

            // Return the final action found by the recursive search
            return action

        } catch (e: Exception) {
            // It's good practice to log the exception
            // Log.e("GraphExecutor", "Error during graph execution", e)
            return null
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
        phonemeMatchStates.clear()
        randomNodeOrders.clear()
        volumeHistory.clear()
    }
}
