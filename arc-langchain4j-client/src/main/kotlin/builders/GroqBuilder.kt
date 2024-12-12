// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.openai.OpenAiChatModel
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.client.langchain4j.LangChainConfig
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<Pair<LangChainConfig, ChatCompletionSettings?>, ChatLanguageModel>()

/**
 * Builds a groq client.
 */
fun groqBuilder(): (LangChainConfig, ChatCompletionSettings?) -> ChatLanguageModel {
    return { model, settings ->
        cache.computeIfAbsent(model to settings) {
            OpenAiChatModel
                .builder()
                .baseUrl(model.url ?: "https://api.groq.com/openai/v1")
                .modelName(model.modelName)
                .apiKey(model.apiKey ?: error("API key is required for Groq!"))
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
}
