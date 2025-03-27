// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatRequestMessage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.functions.toJsonString
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags

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
}

@Serializable
data class InputMessage(
    @SerialName("message.role") val role: String,
    @SerialName("message.content") val content: String,
)
