// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ConversationAgent
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("HandoverHandler")

/**
 * Handles the agent handover process.
 * If the conversation contains an [AIAgentHandover] classification,
 * it will be handed over to the specified agent. Otherwise, it the original result is returned.
 *
 * This function will have no effect if an [AgentProvider] is not available.
 */
suspend fun ConversationAgent.executeWithHandover(
    input: Conversation,
    context: Set<Any>,
    agentProvider: AgentProvider,
): Result<Conversation, AgentFailedException> {
    val handoverLimit = context.filterIsInstance<AgentHandoverLimit>().firstOrNull() ?: AgentHandoverLimit(max = 20)
    val output = execute(input, context + handoverLimit)
    return handleAIAgentHandover(output, input, context, agentProvider, handoverLimit)
}

private suspend fun ConversationAgent.handleAIAgentHandover(
    output: Result<Conversation, AgentFailedException>,
    input: Conversation,
    context: Set<Any>,
    agentProvider: AgentProvider,
    agentHandoverLimit: AgentHandoverLimit,
): Result<Conversation, AgentFailedException> {
    /**
     * Handles agent to agent handover.
     */
    if (agentHandoverLimit.current > agentHandoverLimit.max) {
        log.error("Recursion limit (${agentHandoverLimit.max}) reached for agent handover! Stopping here and returning current result.")
        return output
    }
    output.getOrNull()?.takeIf { it.classification is AIAgentHandover }?.let { conversation ->
        val handover = conversation.classification as AIAgentHandover
        log.info("Agent handover to $handover - $agentHandoverLimit")
        val nextAgent = if (handover.name == this.name) {
            this
        } else {
            agentProvider.getAgentByName(handover.name) as? ConversationAgent?
        }
        if (nextAgent != null) {
            val updatedCount = agentHandoverLimit.increment()
            val newInput = conversation.copy(classification = null)
            val newOutput =
                nextAgent.execute(newInput, context + updatedCount)
            return nextAgent.handleAIAgentHandover(newOutput, newInput, context, agentProvider, updatedCount)
        }
        return output
    }

    /**
     * Handles the agent chain.
     */
    context.filterIsInstance<AgentChain>().firstOrNull()?.let { chain ->
        if (output is Failure) {
            val nextAgentName = chain.agentOnFailure ?: return output
            val nextAgent = agentProvider.getAgentByName(nextAgentName) as? ConversationAgent?
            if (nextAgent != null) {
                val newOutput = nextAgent.execute(input, context)
                return nextAgent.handleAIAgentHandover(newOutput, input, context, agentProvider, agentHandoverLimit)
            }
        } else if (output is Success) {
            val nextAgentName = chain.nextAgent(name) ?: return output
            val nextAgent = agentProvider.getAgentByName(nextAgentName) as? ConversationAgent?
            if (nextAgent != null) {
                val newInput = output.value
                val newOutput = nextAgent.execute(newInput, context)
                return nextAgent.handleAIAgentHandover(newOutput, newInput, context, agentProvider, agentHandoverLimit)
            }
            return Success(output.value.copy(classification = AIAgentHandover(nextAgentName)))
        }
    }

    return output
}

/**
 * The maximum number of times an agent can hand over the conversation to another agent.
 * This is used to prevent infinite loops in the handover process.
 */
data class AgentHandoverLimit(val max: Int, val current: Int = 0) {
    fun increment() = copy(current = current + 1)
}
