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
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.provideOptional
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer
import org.eclipse.lmos.arc.agents.tracing.Events
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.LoggerFactory.getLogger
import org.slf4j.MDC
import java.io.PrintWriter
import java.io.StringWriter
import java.util.Collections.synchronizedList

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
    context: DSLContext,
    fn: suspend (Tags, Events) -> T,
): T {
    val name = agent.name
    return withSpan("agent $name", mapOf(AGENT_LOG_CONTEXT_KEY to (MDC.get("agent") ?: name))) { tags, events ->
        tags.tag("version", agent.version)
        tags.tag("description", agent.description)
        tags.tag("input.value", input.transcript.lastOrNull()?.content ?: "")
        tags.tag("input.mime_type", "text/plain")
        tags.tag("openinference.span.kind", "AGENT")
        tags.tag("conversation", input.conversationId)
        tags.tag("session.id", input.conversationId)
        input.user?.id?.let { tags.tag("user.id", it) }
        AgentTaggers.tagAgent(tags, input, context)
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
                "AGENT_HANDOVER"
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

/**
 * Sets the tracing attributes for the output values and extracts a use case ID if present.
 * The content type defaults to "text/plain" if not specified.
 */
suspend fun Tags.outputWithUseCase(output: String?, contentType: String = "text/plain", context: DSLContext) {
    tag("output.value", output ?: "")
    tag("output.mime_type", contentType)
    val useCaseId = output?.let { "<ID:(.*?)>".toRegex(RegexOption.IGNORE_CASE).find(it)?.groupValues?.get(1) } ?: ""
    tag("usecase.id", useCaseId)
    tag("usecase.content", useCaseId)
    AgentTaggers.tagResponse(this, output, context)
}

/**
 * Sets the tracing attribute for the user ID.
 */
fun Tags.userId(userId: String) {
    tag("user.id", userId)
}

/**
 * A singleton to register global "taggers". These Taggers can add tracing tags to the "generate response" span.
 */
object AgentTaggers {

    private val log = getLogger(this::class.java)

    private val responseTaggers =
        synchronizedList(mutableListOf<suspend (String?, DSLContext) -> Map<String, String>>())

    private val agentTaggers =
        synchronizedList(mutableListOf<suspend (Conversation, DSLContext) -> Map<String, String>>())

    fun addToAgent(fn: suspend (Conversation, DSLContext) -> Map<String, String>) {
        agentTaggers.add(fn)
    }

    fun addToGenerateResponse(fn: suspend (String?, DSLContext) -> Map<String, String>) {
        responseTaggers.add(fn)
    }

    suspend fun tagAgent(tags: Tags, input: Conversation, context: DSLContext) {
        try {
            val newTags =
                agentTaggers.flatMap { it(input, context).entries }.associateBy({ it.key }, { it.value })
            newTags.forEach { (key, value) -> tags.tag(key, value) }
        } catch (ex: Exception) {
            log.error("Error in agent tagger!", ex)
        }
    }

    suspend fun tagResponse(tags: Tags, outputMessage: String?, context: DSLContext) {
        try {
            val newTags =
                responseTaggers.flatMap { it(outputMessage, context).entries }.associateBy({ it.key }, { it.value })
            newTags.forEach { (key, value) -> tags.tag(key, value) }
        } catch (ex: Exception) {
            log.error("Error in generate response tagger!", ex)
        }
    }
}
