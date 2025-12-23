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
 * This executor is stateless. On every tick, it starts from the beginning of the graph
 * and traverses it to determine the correct state.
 */
class GraphExecutor(private val graph: NodeGraph) {

    /**
     * Executes the graph's logic, starting from the graph's start node.
     * It checks for global "ANY STATE" transitions first, then proceeds with normal
     * graph traversal until an action is returned or a branch ends.
     * @param context The live data from the ViewModel.
     * @return An action to be performed, or null if no action is required.
     */
    fun tick(context: GraphExecutionContext): GraphAction? {
        val nextNodeId: NodeId? = graph.startNodeId

        var currentNode = nextNodeId?.let { graph.nodes[it] }

        if (currentNode == null) {
            if (graph.startNodeId != null) {
                println("GraphExecutor: Start node with id ${graph.startNodeId} not found in graph.")
            } else {
                println("GraphExecutor: No start node defined for the graph.")
            }
            return null // Can't execute without a starting node.
        }

        println("GraphExecutor: Tick starting from node ${currentNode.id}")

        while (currentNode != null) {
            println("GraphExecutor: Executing node ${currentNode.id}")
            val result = currentNode.execute(context, graph)

            if (result.action != null) {
                println("GraphExecutor: Action triggered: ${result.action}")
                // An action was found, so we're done for this tick.
                return result.action
            }

            if (result.nextNodeId != null) {
                println("GraphExecutor: Transitioning to node ${result.nextNodeId}")
                currentNode = graph.nodes[result.nextNodeId]
            } else {
                // End of a branch.
                println("GraphExecutor: End of branch reached at node ${currentNode.id}")
                break
            }
        }

        println("GraphExecutor: Tick finished without producing an action.")
        return null
    }

    /**
     * Resets the executor. As the executor is now stateless, this method is a no-op
     * but is kept for API compatibility.
     */
    fun reset() {
        // No-op. The executor is stateless and resets on every tick automatically.
    }
}
