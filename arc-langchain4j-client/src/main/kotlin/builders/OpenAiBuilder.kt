// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.openai.OpenAiResponsesChatModel
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.OutputFormat

/**
 * Builds an OpenAI Responses API client.
 */
fun openAiBuilder(): (AIClientConfig, ChatCompletionSettings?) -> ChatModel {
    return { config, settings ->
        OpenAiResponsesChatModel.builder()
            .apiKey(config.apiKey ?: error("API key is required for OpenAI!"))
            .modelName(config.modelName ?: settings?.deploymentNameOrModel())
            .parallelToolCalls(true)
            .maxToolCalls(1000)
            .apply {
                config.endpoint?.let { baseUrl(it) }
                settings?.temperature?.let { temperature(it) }
                settings?.topP?.let { topP(it) }
                settings?.maxTokens?.let { maxOutputTokens(it) }
                settings?.serviceTier?.let { serviceTier(it) }
                settings?.reasoningEffort?.let { reasoningEffort(it.name.lowercase()) }
                settings?.format?.let {
                    responseFormat(
                        when (it) {
                            OutputFormat.TEXT -> ResponseFormat.TEXT
                            OutputFormat.JSON -> ResponseFormat.JSON
                        },
                    )
                }
            }
            .build()
    }
}

