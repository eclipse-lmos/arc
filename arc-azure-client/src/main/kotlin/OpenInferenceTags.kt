// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatCompletionsFunctionToolCall
import com.azure.ai.openai.models.ChatRequestAssistantMessage
import com.azure.ai.openai.models.ChatRequestMessage
import com.azure.ai.openai.models.ChatRequestSystemMessage
import com.azure.ai.openai.models.ChatRequestToolMessage
import com.azure.ai.openai.models.ChatRequestUserMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.MDC

/**
 * Helper object to apply attributes to the tags following the OpenInference spec.
 * https://github.com/Arize-ai/openinference/tree/main/spec
 */
object OpenInferenceTags {

    fun applyAttributes(
        tags: Tags,
        config: AIClientConfig,
        settings: ChatCompletionSettings?,
        completions: ChatCompletions,
        inputMessages: List<ChatRequestMessage>,
        functionCallHandler: FunctionCallHandler,
    ) {
        tags.tag("openinference.span.kind", "LLM")
        tags.tag("llm.model_name", config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown")
        tags.tag("llm.provider", "azure")
        tags.tag("llm.system", "openai")
        if (completions.choices.isNotEmpty()) {
            tags.tag(
                "finish_reason",
                completions.choices[0].finishReason?.value ?: "unknown",
            )
        }
        settings?.let {
            tags.tag(
                "llm.invocation_parameters",
                """
                {"model_name": "${config.modelName ?: settings.deploymentNameOrModel() ?: "unknown"}", "temperature": "${it.temperature}", "seed": "${it.seed}"}
                """.trimIndent(),
            )
        }

        tags.tag("input.mime_type", "text/plain")
        inputMessages.forEachIndexed { i, message ->
            val content = when (message) {
                is ChatRequestUserMessage -> message.content
                is ChatRequestAssistantMessage -> message.content
                is ChatRequestSystemMessage -> message.content
                is ChatRequestToolMessage -> message.content
                else -> null
            }
            tags.tag("llm.input_messages.$i.message.role", message.role.toString())
            if (content != null) {
                tags.tag("llm.input_messages.$i.message.content", content.toString())
            }
            if (i == inputMessages.size - 1) {
                tags.tag("input.value", content.toString())
            }
        }
        completions.choices.filter { it?.message != null }.forEachIndexed { i, choice ->
            tags.tag("llm.output_messages.$i.message.role", choice.message.role.toString())
            if (choice.message.content != null) {
                tags.tag("llm.output_messages.$i.message.content", choice.message.content)
            }
            choice.message.toolCalls?.forEachIndexed { y, call ->
                val toolCall = call as ChatCompletionsFunctionToolCall
                tags.tag("llm.output_messages.$i.message.tool_calls.$y.tool_call.function.name", toolCall.function.name)
                tags.tag(
                    "llm.output_messages.$i.message.tool_calls.$y.tool_call.function.arguments",
                    toolCall.function.arguments,
                )
            }
        }
        functionCallHandler.functions.forEachIndexed { i, tool ->
            tags.tag("llm.tools.$i.tool.name", tool.name)
            tags.tag(
                "llm.tools.$i.tool.json_schema",
                """{"type":"function","function":{"name":"${tool.name}","parameters":${tool.parameters.toJsonString()},"description":"${tool.description}"}}""".replace(
                    "\n",
                    " ",
                ),
            )
        }

        tags.tag("output.value", completions.choices.firstOrNull()?.message?.content ?: "")
        tags.tag("output.mime_type", "text/plain") // TODO
        tags.tag("llm.token_count.prompt", completions.usage.promptTokens.toLong())
        tags.tag("llm.token_count.completion", completions.usage.completionTokens.toLong())
        tags.tag("llm.token_count.total", completions.usage.totalTokens.toLong())
    }

    fun applyToolAttributes(
        functionName: String,
        toolCall: ChatCompletionsFunctionToolCall,
        tags: Tags,
    ) {
        val functionArguments = toolCall.function.arguments
        tags.tag("openinference.span.kind", "TOOL")
        tags.tag("tool_call.id", toolCall.id)
        tags.tag("tool_call.function.name", functionName)
        tags.tag("tool_call.function.arguments", functionArguments)
        tags.tag("input.value", functionArguments)
        tags.tag("input.mime_type", "application/json")
        MDC.get("use_case")?.let { tags.tag("use_case", it) }
    }

    fun applyToolAttributes(function: LLMFunction, tags: Tags) {
        tags.tag("tool.name", function.name)
        tags.tag("tool.id", function.name)
        tags.tag("tool.description", function.description)
        tags.tag("tool.parameters", function.parameters.toJsonString())
        tags.tag(
            "tool.json_schema",
            """{"type":"function","function":{"name":"${function.name}","parameters":${function.parameters.toJsonString()},"description":"${function.description}"}}""".replace(
                "\n",
                " ",
            ),
        )
    }

    fun applyToolAttributes(result: Result<String, ArcException>, tags: Tags) {
        tags.tag("output.value", result.getOrNull() ?: "")
    }
}

@Serializable
data class InputMessage(
    @SerialName("message.role") val role: String,
    @SerialName("message.content") val content: String,
)
