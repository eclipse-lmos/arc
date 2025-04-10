// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.AGENT_LOG_CONTEXT_KEY
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.provideOptional
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer
import org.eclipse.lmos.arc.agents.tracing.Events
import org.eclipse.lmos.arc.agents.tracing.Tags

/**
 * Provides an [AgentTracer] instance.
 * If no [AgentTracer] is available, a [DefaultAgentTracer] is returned.
 *
 * @return the [AgentTracer] instance
 */
suspend fun BeanProvider.agentTracer() = provideOptional<AgentTracer>() ?: DefaultAgentTracer()

/**
 * Sets the agent tracing attributes to the given function.
 */
suspend fun <T> AgentTracer.withAgentSpan(
    name: String,
    input: Conversation,
    fn: suspend (Tags, Events) -> T,
): T {
    return withSpan("agent $name", mapOf(AGENT_LOG_CONTEXT_KEY to name)) { tags, events ->
        tags.tag("input.value", input.transcript.lastOrNull()?.content ?: "")
        tags.tag("input.mime_type", "text/plain")
        tags.tag("openinference.span.kind", "AGENT")
        tags.tag("conversation", input.conversationId)
        tags.tag("session.id", input.conversationId)
        fn(tags, events)
    }
}
