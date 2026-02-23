// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.openai

import com.openai.models.chat.completions.ChatCompletion
import com.openai.models.chat.completions.ChatCompletionMessageParam
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.Tags

/**
 * Helper object to apply attributes to the tags following the Opentelemetry Gen AI spec.
 * https://opentelemetry.io/docs/specs/semconv/gen-ai/
 */
object GenAITags {

    fun applyAttributes(
        tags: Tags,
        config: AIClientConfig,
        settings: ChatCompletionSettings?,
        completions: ChatCompletion,
        inputMessages: List<ChatCompletionMessageParam>,
    ) {
        tags.tag("gen_ai.request.model", config.modelName ?: settings?.deploymentNameOrModel() ?: "unknown")
        tags.tag("gen_ai.operation.name", "chat")
        tags.tag(
            "gen_ai.response.finish_reasons",
            completions.choices().joinToString(
                prefix = "[",
                postfix = "]",
                separator = ",",
            ) { it.finishReason().toString() },
        )
        settings?.seed?.let { tags.tag("gen_ai.request.seed", it.toString()) }
        settings?.temperature?.let { tags.tag("gen_ai.request.temperature", it.toString()) }
        settings?.topP?.let { tags.tag("gen_ai.request.top_p", it.toString()) }

        val usage = completions.usage()
        if (usage.isPresent) {
            val u = usage.get()
            tags.tag("gen_ai.usage.input_tokens", u.promptTokens())
            tags.tag("gen_ai.usage.output_tokens", u.completionTokens())
        }
        completions.systemFingerprint().ifPresent { tags.tag("gen_ai.openai.response.system_fingerprint", it) }
    }
}
