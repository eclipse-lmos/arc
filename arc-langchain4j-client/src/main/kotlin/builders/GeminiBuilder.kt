// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.OutputFormat
import org.eclipse.lmos.arc.client.langchain4j.LangChainConfig
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<Pair<LangChainConfig, ChatCompletionSettings?>, ChatLanguageModel>()

/**
 * Builds a Gemini client that uses an API key for authentication.
 */
fun geminiBuilder(): (LangChainConfig, ChatCompletionSettings?) -> ChatLanguageModel {
    return { model, settings ->
        cache.computeIfAbsent(model to settings) {
            GoogleAiGeminiChatModel.builder()
                .apiKey(model.apiKey ?: error("API key is required for Gemini!"))
                .modelName(model.modelName)
                .apply {
                    if (settings != null) {
                        settings.topP?.let { topP(it) }
                        settings.temperature?.let { temperature(it) }
                        settings.maxTokens?.let { maxOutputTokens(it) }
                        settings.topK?.let { topK(it) }
                        settings.format?.let {
                            responseFormat(
                                when (it) {
                                    OutputFormat.TEXT -> ResponseFormat.TEXT
                                    OutputFormat.JSON -> ResponseFormat.JSON
                                },
                            )
                        }
                    }
                }
                .build()
        }
    }
}
