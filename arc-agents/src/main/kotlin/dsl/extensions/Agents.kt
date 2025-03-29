// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.AgentDefinition
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result

/**
 * If set, the system will hand over the conversation to the specified agent.
 */
fun OutputFilterContext.nextAgent(name: String) {
    output = output.copy(classification = AIAgentHandover(name))
}

fun AgentDefinition.nextAgent(name: String) {
    filterOutput {
        nextAgent(name)
    }
}

/**
 * Calls the specified agent with the current conversation.
 * The context can be used to pass additional information to the agent.
 *
 * The Agent that is called must be a ChatAgent and must be registered in the AgentProvider.
 */
suspend fun DSLContext.callAgent(
    name: String,
    conversation: Conversation? = null,
    context: Set<Any> = emptySet(),
) = result<Conversation, AgentFailedException> {
    val agent = get<AgentProvider>().getAgentByName(name) as? ChatAgent
        ?: failWith { AgentFailedException("Unknown agent '$name'!") }
    return agent.execute(input = conversation ?: get<Conversation>(), context = context)
}
