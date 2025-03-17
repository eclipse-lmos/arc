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
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags

/**
 * Helper object to apply attributes to the tags for OpenAI inference.
 */
object OpenInference {

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
        tags.tag("llm.input_messages", Json.encodeToString(messages))
        tags.tag("output.value", "assistant: ${completions.choices.first().message.content}")
        tags.tag("output.mime_type", "text/plain") // TODO
    }
}

@Serializable
data class InputMessage(
    @SerialName("message.role") val role: String,
    @SerialName("message.content") val content: String,
)
