// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.models.ChatCompletions
import com.azure.ai.openai.models.ChatRequestMessage
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags

/**
 * Helper object to apply attributes to the tags following the Opentelemetry Gen AI spec.
 * https://opentelemetry.io/docs/specs/semconv/gen-ai/
 */
object GenAITags {

    fun applyAttributes(
        tags: Tags,
        config: AzureClientConfig,
        settings: ChatCompletionSettings?,
        completions: ChatCompletions,
        inputMessages: List<ChatRequestMessage>,
    ) {
        tags.tag("gen_ai.request.model", config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown")
        tags.tag("gen_ai.operation.name", "chat")
        tags.tag(
            "gen_ai.response.finish_reasons",
            completions.choices.joinToString(
                prefix = "[",
                postfix = "]",
                separator = ",",
            ) { it.finishReason.toString() },
        )
        settings?.seed?.let { tags.tag("gen_ai.request.seed", it) }
        settings?.temperature?.let { tags.tag("gen_ai.request.temperature", it.toString()) }
        settings?.topP?.let { tags.tag("gen_ai.request.top_p", it.toString()) }
        // tags.tag("gen_ai.user.message", event.messages.last().content)
        // tags.tag("gen_ai.choice", event.result.getOrNull()?.content ?: "")
        tags.tag("gen_ai.usage.input_tokens", completions.usage.promptTokens.toLong())
        tags.tag("gen_ai.usage.output_tokens", completions.usage.completionTokens.toLong())
        tags.tag("gen_ai.openai.response.system_fingerprint", completions.systemFingerprint)
    }
}
