// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

/**
 * Represents a list of agents that should be executed one after the other
 * with the output of the previous agent being the input of the next agent.
 */
data class AgentChain(val agents: List<String>, val agentOnFailure: String? = null) {

    init {
        require(agents.isNotEmpty()) { "Agent chain must contain at least one agent." }
    }

    /**
     * Returns the next agent in the chain.
     * If the current agent is the last one in the chain, it returns null.
     * If the current agent is not found in the chain, it returns null.
     */
    fun nextAgent(currentAgent: String): String? {
        val currentIndex = agents.indexOf(currentAgent)
        if (currentIndex == -1) return null
        return if (currentIndex < agents.size - 1) {
            agents[currentIndex + 1]
        } else {
            null
        }
    }
}
