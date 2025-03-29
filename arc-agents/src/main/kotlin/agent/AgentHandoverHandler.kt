// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ChatAgent
import org.eclipse.lmos.arc.agents.conversation.AIAgentHandover
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.getAgentByName
import org.eclipse.lmos.arc.core.Result
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
suspend fun ChatAgent.executeWithHandover(
    input: Conversation,
    context: Set<Any>,
    agentProvider: AgentProvider,
): Result<Conversation, AgentFailedException> {
    val handoverLimit = context.filterIsInstance<AgentHandoverLimit>().firstOrNull() ?: AgentHandoverLimit(max = 20)
    val output = execute(input, context + handoverLimit)
    return handleAIAgentHandover(output, context, agentProvider, handoverLimit)
}

private suspend fun ChatAgent.handleAIAgentHandover(
    output: Result<Conversation, AgentFailedException>,
    context: Set<Any>,
    agentProvider: AgentProvider,
    agentHandoverLimit: AgentHandoverLimit,
): Result<Conversation, AgentFailedException> {
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
            agentProvider.getAgentByName(handover.name) as? ChatAgent?
        }
        if (nextAgent != null) {
            val updatedCount = agentHandoverLimit.increment()
            val newOutput =
                nextAgent.execute(conversation.copy(classification = null), context + updatedCount)
            return handleAIAgentHandover(newOutput, context, agentProvider, updatedCount)
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
