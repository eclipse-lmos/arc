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
    ) {
        val messages = inputMessages.mapNotNull {
            when (it) {
                is ChatRequestAssistantMessage -> InputMessage(it.role.toString(), it.content.toString())
                is ChatRequestUserMessage -> InputMessage(it.role.toString(), it.content.toString())
                is ChatRequestSystemMessage -> InputMessage(it.role.toString(), it.content.toString())
                else -> null
            }
        }
        // tags.tag("llm.input_messages",  )
        tags.tag("llm.model_name", completions.model)
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
