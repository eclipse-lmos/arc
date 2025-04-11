// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatRequestAssistantMessage
import com.azure.ai.openai.models.ChatRequestMessage
import com.azure.ai.openai.models.ChatRequestSystemMessage
import com.azure.ai.openai.models.ChatRequestUserMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.ArcException
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.getOrNull

/**
 * Helper object to apply attributes to the tags following the OpenInference spec.
 * https://github.com/Arize-ai/openinference/tree/main/spec
 */
object OpenInferenceTags {

    fun applyAttributes(
        tags: Tags,
        config: AzureClientConfig,
        settings: ChatCompletionSettings?,
        completions: ChatCompletions,
        inputMessages: List<ChatRequestMessage>,
        functionCallHandler: FunctionCallHandler,
    ) {
        tags.tag("openinference.span.kind", "LLM")
        tags.tag("llm.model_name", config.modelName)
        tags.tag("llm.provider", "azure")
        tags.tag("llm.system", "openai")
        settings?.let {
            tags.tag(
                "llm.invocation_parameters",
                """
                {"model_name": "${config.modelName}", "temperature": "${it.temperature}", "seed": "${it.seed}"}
                """.trimIndent(),
            )
        }

        tags.tag("input.mime_type", "text/plain")
        inputMessages.forEachIndexed { i, message ->
            val content = when (message) {
                is ChatRequestUserMessage -> message.content
                is ChatRequestAssistantMessage -> message.content
                is ChatRequestSystemMessage -> message.content
                else -> null
            }
            if (content != null) {
                tags.tag("llm.input_messages.$i.message.role", message.role.toString())
                tags.tag("llm.input_messages.$i.message.content", content.toString())
            }
            if (i == inputMessages.size - 1) {
                tags.tag("input.value", content.toString())
            }
        }
        completions.choices.filter { it?.message?.content != null }.forEachIndexed { i, choice ->
            tags.tag("llm.output_messages.$i.message.role", choice.message.role.toString())
            tags.tag("llm.output_messages.$i.message.content", choice.message.content)
        }
        functionCallHandler.functions.forEachIndexed { i, tool ->
            tags.tag("llm.tools.$i.tool.name", tool.name)
            tags.tag("llm.tools.$i.tool.json_schema", tool.parameters.toJsonString())
        }

        tags.tag("output.value", "assistant: ${completions.choices.first().message.content}")
        tags.tag("output.mime_type", "text/plain") // TODO
        tags.tag("llm.token_count.prompt", completions.usage.promptTokens.toLong())
        tags.tag("llm.token_count.completion", completions.usage.completionTokens.toLong())
        tags.tag("llm.token_count.total", completions.usage.totalTokens.toLong())
    }

    fun applyToolAttributes(
        functionName: String,
        functionArguments: String,
        tags: Tags,
    ) {
        tags.tag("openinference.span.kind", "TOOL")
        tags.tag("tool_call.function.name", functionName)
        tags.tag("tool_call.function.arguments", functionArguments)
        tags.tag("input.value", functionArguments)
        tags.tag("input.mime_type", "application/json")
    }

    fun applyToolAttributes(function: LLMFunction, tags: Tags) {
        tags.tag("tool.name", function.name)
        tags.tag("tool.description", function.description)
        tags.tag("tool.parameters", function.parameters.toJsonString())
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
