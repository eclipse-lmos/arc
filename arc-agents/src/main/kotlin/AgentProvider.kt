// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import java.util.*

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

/**
 * Implementation of the [AgentLoader] that is backed by a list of [Agent]s.
 */
class ListAgentLoader : AgentLoader {

    private val allAgents = Vector<Agent<*, *>>()

    override fun getAgents() = allAgents

    fun addAll(agents: List<Agent<*, *>>) {
        allAgents.addAll(agents)
    }
}
