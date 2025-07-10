// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.agent

import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agents.AGENT_LOG_CONTEXT_KEY
import org.eclipse.lmos.arc.agents.Agent
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
import org.slf4j.MDC
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
    agent: Agent<*, *>,
    input: Conversation,
    fn: suspend (Tags, Events) -> T,
): T {
    val name = agent.name
    return withSpan("agent $name", mapOf(AGENT_LOG_CONTEXT_KEY to (MDC.get("agent") ?: name))) { tags, events ->
        tags.tag("version", agent.version)
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
 * Sets the tracing attributes for a "CHAIN" span.
 */
suspend fun <T> AgentTracer.spanChain(
    name: String,
    attributes: Map<String, String> = emptyMap(),
    fn: suspend (Tags, Events) -> T,
): T {
    return withSpan(name, attributes) { tags, events ->
        tags.tag("openinference.span.kind", "CHAIN")
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

    val status: String = when (result) {
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
    val mdc: Map<String, String> = MDC.getCopyOfContextMap() ?: emptyMap()
    val details: Map<String, String> = mapOf("status" to status, "flowBreak" to "$flowBreak")
    tag("metadata", Json.encodeToString(mdc + details))
}

/**
 * Sets the tracing attributes for the prompt version.
 */
fun Tags.promptVersion(version: String?) {
    if (version != null) tag("llm.prompt_template.version", version)
}

/**
 * Sets the tracing attributes for the input values.
 * The content type defaults to "text/plain" if not specified.
 */
fun Tags.input(input: String?, contentType: String = "text/plain") {
    tag("input.value", input ?: "")
    tag("input.mime_type", contentType)
}

/**
 * Sets the tracing attributes for the output values.
 * The content type defaults to "text/plain" if not specified.
 */
fun Tags.output(output: String?, contentType: String = "text/plain") {
    tag("output.value", output ?: "")
    tag("output.mime_type", contentType)
}
