// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j

import dev.langchain4j.data.message.AiMessage
import dev.langchain4j.data.message.ChatMessage
import dev.langchain4j.data.message.SystemMessage
import dev.langchain4j.data.message.ToolExecutionResultMessage
import dev.langchain4j.data.message.UserMessage
import dev.langchain4j.model.output.Response
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer
import org.eclipse.lmos.arc.agents.tracing.Events
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.getOrNull
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind.EXACTLY_ONCE
import kotlin.contracts.contract

/**
 * Helper object to apply attributes to the tags following the OpenInference spec.
 * https://github.com/Arize-ai/openinference/tree/main/spec
 */

/**
 * Helper function to create a span for LLM calls.
 */
@OptIn(ExperimentalContracts::class)
suspend fun <T> AgentTracer?.spanLLMCall(fn: suspend (Tags, Events) -> T): T {
    contract {
        callsInPlace(fn, EXACTLY_ONCE)
    }
    return (this ?: DefaultAgentTracer()).withSpan("llm") { tags, events ->
        fn(tags, events)
    }
}

/**
 * Helper function to add the LLM Attributes to the llm span.
 */
fun Tags.addLLMTags(
    config: AIClientConfig,
    settings: ChatCompletionSettings?,
    inputMessages: List<ChatMessage>,
    outputMessages: List<Response<AiMessage>>,
    functions: List<LLMFunction>,
    usage: Usage,
) {
    tag("openinference.span.kind", "LLM")
    tag("llm.model_name", config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown")
    tag("llm.provider", "langchain4j")
    tag("llm.system", config.client)
    settings?.let {
        tag(
            "llm.invocation_parameters",
            """
                {"model_name": "${config.modelName ?: settings.deploymentNameOrModel() ?: "unknown"}", "temperature": "${it.temperature}", "seed": "${it.seed}"}
            """.trimIndent(),
        )
    }

    tag("input.mime_type", "text/plain")
    inputMessages.forEachIndexed { i, message ->
        val role = when (message) {
            is UserMessage -> "user"
            is AiMessage -> "assistant"
            is SystemMessage -> "system"
            is ToolExecutionResultMessage -> "tool"
            else -> "unknown"
        }
        tag("llm.input_messages.$i.message.role", role)
        if (message.text() != null) {
            tag("llm.input_messages.$i.message.content", message.text())
        }
        if (i == inputMessages.size - 1) {
            tag("input.value", message.text() ?: "")
        }
    }
    outputMessages.forEachIndexed { i, response ->
        val message = response.content()
        tag("llm.output_messages.$i.message.role", "assistant")
        tag("llm.output_messages.$i.message.content", message.text() ?: "")
        message.toolExecutionRequests()?.forEachIndexed { y, toolCall ->
            tag("llm.output_messages.$i.message.tool_calls.$y.tool_call.function.name", toolCall.name())
            tag(
                "llm.output_messages.$i.message.tool_calls.$y.tool_call.function.arguments",
                toolCall.arguments(),
            )
        }
    }
    functions.forEachIndexed { i, tool ->
        tag("llm.tools.$i.tool.name", tool.name)
        tag(
            "llm.tools.$i.tool.json_schema",
            """{"type":"function","function":{"name":"${tool.name}","parameters":${tool.parameters.toJsonString()},"description":"${tool.description}"}}""",
        )
    }

    tag("output.value", outputMessages.firstOrNull()?.content()?.text() ?: "")
    tag("output.mime_type", "text/plain") // TODO
    tag("llm.token_count.prompt", usage.promptCount.toLong())
    tag("llm.token_count.completion", usage.completionCount.toLong())
    tag("llm.token_count.total", usage.totalCount.toLong())
}

@OptIn(ExperimentalContracts::class)
suspend fun <T> AgentTracer?.spanToolCall(fn: suspend (Tags, Events) -> T): T {
    contract {
        callsInPlace(fn, EXACTLY_ONCE)
    }
    return (this ?: DefaultAgentTracer()).withSpan("tool") { tags, events ->
        fn(tags, events)
    }
}

fun Tags.addToolTags(
    function: LLMFunction,
    functionArguments: String,
) {
    tag("openinference.span.kind", "TOOL")
    tag("tool_call.function.name", function.name)
    tag("tool_call.function.arguments", functionArguments)
    tag("input.value", functionArguments)
    tag("input.mime_type", "application/json")
    tag("tool.name", function.name)
    tag("tool.id", function.name)
    tag("tool.description", function.description)
    tag("tool.parameters", function.parameters.toJsonString())
}

fun Tags.addToolOutput(result: Result<String, ArcException>) {
    tag("output.value", result.getOrNull() ?: "")
}

data class Usage(
    val promptCount: Int,
    val completionCount: Int,
    val totalCount: Int,
)
