package org.menagerie.puppet_master.state_machine

/**
 * A data container that provides the live values that a graph needs to execute its logic.
 * This object is created and updated by the MainViewModel.
 */
data class GraphExecutionContext(
    val microphoneVolume: Float = 0f,
    val hotKeyPressed: String? = null
    // Future live data can be added here, e.g., timers, audience metrics, etc.
)

/**
 * Represents an action that the UI should take based on the graph's execution.
 */
sealed interface GraphAction {
    data class SetState(val stateName: String) : GraphAction
}

/**
 * Executes the logic of a NodeGraph based on a given ExecutionContext.
 */
class GraphExecutor(private val graph: NodeGraph) {

    private var currentNodeId: NodeId? = graph.startNodeId

    /**
     * Executes the graph's logic, starting from the current node.
     * It will continue processing nodes until a node returns an action or the end of a branch is reached.
     * @param context The live data from the ViewModel.
     * @return An action to be performed, like changing the puppet's state, or null if no action is required.
     */
    fun tick(context: GraphExecutionContext): GraphAction? {
        // Find all "ANY STATE" nodes and check their outbound connections for triggered conditionals.
        val anyStateNodes = graph.nodes.values.filter { it is SetStateNode && it.stateName == ANY_STATE }
        for (node in anyStateNodes) {
            val wires = graph.wires.filter { it.fromNodeId == node.id }
            for (wire in wires) {
                val nextNode = graph.nodes[wire.toNodeId]
                if (nextNode != null) {
                    val result = nextNode.execute(context, graph)
                    if (result.nextNodeId != null) {
                        // A global transition was triggered. Jump to that path.
                        currentNodeId = result.nextNodeId
                        break
                    }
                }
            }
        }

        var currentNode = currentNodeId?.let { graph.nodes[it] }

        while (currentNode != null) {
            val result = currentNode.execute(context, graph)

            currentNodeId = result.nextNodeId
            currentNode = currentNodeId?.let { graph.nodes[it] }

            if (result.action != null) {
                return result.action
            }
        }

        return null
    }

    fun reset() {
        currentNodeId = graph.startNodeId
    }
}
