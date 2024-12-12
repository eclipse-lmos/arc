// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

/**
 * Provides Agents to other components to an application.
 * Usually there is one instance of this class per application.
 */
fun interface AgentProvider {

    fun getAgents(): List<Agent<*, *>>
}

/**
 * Loads Agents.
 * Typically, a [AgentProvider] uses [AgentLoader]s to load Arc Agent from different sources.
 * There can be many implementations of [AgentLoader]s in an application.
 */
fun interface AgentLoader {

    fun getAgents(): List<Agent<*, *>>
}

/**
 * Returns the agent with the given name or null if no agent with that name exists.
 */
fun AgentProvider.getAgentByName(name: String) = getAgents().firstOrNull { it.name == name }

/**
 * Implementation of the [AgentProvider] that combines multiple [AgentLoader]s and a list of [Agent]s.
 */
class CompositeAgentProvider(private val loaders: List<AgentLoader>, private val agents: List<Agent<*, *>>) :
    AgentProvider {

    override fun getAgents(): List<Agent<*, *>> {
        return loaders.flatMap { it.getAgents() } + agents
    }
}
