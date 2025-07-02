// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.conversation.toConversation
import org.eclipse.lmos.arc.agents.dsl.AgentDefinition
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.get
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.map
import org.eclipse.lmos.arc.core.result

/**
 * If set, the system will hand over the conversation to the specified agent.
 */
suspend fun OutputFilterContext.nextAgent(name: String) {
    output = output.copy(classification = AIAgentHandover(name))
    emit(AgentHandOverTriggered(getCurrentAgent()?.name ?: "unknown", name))
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
    input: Conversation,
    context: Set<Any> = emptySet(),
) = result<Conversation, AgentFailedException> {
    if (name == getCurrentAgent()?.name) failWith { AgentFailedException("Agent cannot call itself!") }
    val agent = get<AgentProvider>().getAgentByName(name) as? ConversationAgent
        ?: failWith { AgentFailedException("Unknown agent '$name'!") }
    return agent.execute(input = input, context = context)
}

/**
 * Returns the current agent if available.
 */
fun DSLContext.getCurrentAgent(): Agent<*, *>? {
    return getLocal("agent") as? Agent<*, *>?
}

/**
 * Convenient function to call an agent with a string input.
 */
suspend fun DSLContext.askAgent(
    name: String,
    input: String,
    context: Set<Any> = emptySet(),
): Result<String, AgentFailedException> =
    callAgent(name, input.toConversation(), context).map { it.latest<AssistantMessage>()?.content ?: "" }

/**
 * Cancels the current execution of the Agent and hands over the conversation to the given agent.
 */
suspend fun DSLContext.breakToAgent(name: String, conversation: Conversation? = null, reason: String? = null): Nothing {
    val conversationResult = (conversation ?: get<Conversation>()).copy(classification = AIAgentHandover(name))
    throw InterruptProcessingException(conversationResult, reason)
}

/**
 * Events
 */
class AgentHandOverTriggered(val fromAgent: String, val toAgent: String) : Event by BaseEvent()

/**
 * Returns the current agent.
 */
suspend fun DSLContext.currentAgent(): Agent<*, *>? {
    return getOptional<Agent<*, *>>()
}
