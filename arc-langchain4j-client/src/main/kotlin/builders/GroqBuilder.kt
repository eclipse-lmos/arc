// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.openai.OpenAiChatModel
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings

/**
 * Builds a groq client.
 */
fun groqBuilder(): (AIClientConfig, ChatCompletionSettings?) -> ChatModel {
    return { config, settings ->
        OpenAiChatModel
            .builder()
            .baseUrl(config.endpoint ?: "https://api.groq.com/openai/v1")
            .modelName(config.modelName ?: settings?.model ?: settings?.deploymentName)
            .apiKey(config.apiKey ?: error("API key is required for Groq!"))
            .apply {
                if (settings != null) {
                    settings.topP?.let { topP(it) }
                    settings.temperature?.let { temperature(it) }
                    settings.seed?.let { seed(it.toInt()) }
                }
            }
            .build()
    }
}
