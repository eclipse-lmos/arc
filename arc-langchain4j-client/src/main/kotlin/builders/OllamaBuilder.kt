// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.client.langchain4j.LangChainConfig
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<Pair<LangChainConfig, ChatCompletionSettings?>, ChatLanguageModel>()

/**
 * Builds a ollama client.
 */
fun ollamaBuilder(): (LangChainConfig, ChatCompletionSettings?) -> ChatLanguageModel {
    return { model, settings ->
        cache.computeIfAbsent(model to settings) {
            OllamaChatModel
                .builder()
                .baseUrl(model.url ?: "http://localhost:11434")
                .modelName(model.modelName)
                .apply {
                    if (settings != null) {
                        settings.topP?.let { topP(it) }
                        settings.temperature?.let { temperature(it) }
                        settings.seed?.let { seed(it.toInt()) }
                        settings.topK?.let { topK(it) }
                    }
                }
                .build()
        }
    }
}
