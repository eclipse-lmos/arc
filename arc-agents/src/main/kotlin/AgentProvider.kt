// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.agents.agent.AgentProxy
import org.eclipse.lmos.arc.agents.agent.process
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
 * Returns a typed proxy for a conversation-based Agent matching [Input] and [Output].
 *
 * Input objects are serialized to JSON and Agent responses are deserialized to [Output].
 * Both types must be serializable unless they are strings.
 * If [name] is omitted, exactly one Agent must be registered for the requested type pair.
 *
 * @throws IllegalArgumentException if no matching Agent exists or the type pair is ambiguous.
 */
inline fun <reified Input : Any, reified Output : Any> AgentProvider.getAgent(
    name: String? = null,
): AgentProxy<Input, Output> {
    val matches = getAgents().filterIsInstance<ConversationAgent>().filter { agent ->
        val metadata = agent as? AgentTypeMetadata
        metadata?.inputType == Input::class &&
            metadata.outputType == Output::class &&
            (name == null || agent.name == name)
    }
    val typePair = "${Input::class.simpleName} -> ${Output::class.simpleName}"
    val agent = when (matches.size) {
        1 -> matches.single()
        0 -> throw IllegalArgumentException(
            if (name == null) {
                "No conversation agent registered for type pair $typePair"
            } else {
                "No conversation agent named '$name' registered for type pair $typePair"
            },
        )
        else -> throw IllegalArgumentException(
            "Multiple conversation agents registered for type pair $typePair; specify an agent name",
        )
    }
    return AgentProxy { input -> agent.process<Input, Output>(input) }
}

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
