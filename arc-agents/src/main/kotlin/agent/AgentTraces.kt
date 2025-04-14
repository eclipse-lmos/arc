// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.AGENT_LOG_CONTEXT_KEY
import org.eclipse.lmos.arc.agents.AgentFailedException
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.latest
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.provideOptional
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer
import org.eclipse.lmos.arc.agents.tracing.Events
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import java.io.PrintWriter
import java.io.StringWriter

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
        input.user?.id?.let { tags.tag("user.id", it) }
        fn(tags, events)
    }
}

/**
 * Sets the tracing attributes for an exception.
 */
fun Tags.onError(error: Throwable) {
    val st = StringWriter()
    error.printStackTrace(PrintWriter(st))
    tag("exception.escaped", true)
    tag("exception.message", error.message ?: "")
    tag("exception.stacktrace", st.toString())
    tag("exception.type", error::class.simpleName ?: "")
    error(error)
}

/**
 * Sets the tracing attributes for the final status.
 */
fun Tags.addResultTags(result: Result<Conversation, AgentFailedException>, flowBreak: Boolean = false) {
    tag("output.value", result.getOrNull()?.transcript?.last()?.content ?: "")
    tag("output.mime_type", "text/plain")

    val status = when (result) {
        is Success -> {
            val response = result.value.latest<AssistantMessage>()?.content
            if (result.value.classification != null) {
                result.value.classification.toString()
            } else if (response?.contains("AGENT_HANDOVER") == true) {
                response
            } else {
                when (result.value.latest<AssistantMessage>()?.content) {
                    "UNRESOLVED" -> "UNRESOLVED"
                    "RESOLVED" -> "RESOLVED"
                    "HACKING_DETECTED" -> "HACKING_DETECTED"
                    else -> "ONGOING"
                }
            }
        }

        is Failure -> "FAILURE"
    }
    tag("status", status)
    tag("metadata", """{"status": "$status", "flowBreak": $flowBreak}""")
}
