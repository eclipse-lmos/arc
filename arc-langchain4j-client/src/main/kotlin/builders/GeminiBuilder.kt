// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ResponseFormat
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.OutputFormat

/**
 * Builds a Gemini client that uses an API key for authentication.
 */
fun geminiBuilder(): (AIClientConfig, ChatCompletionSettings?) -> ChatModel {
    return { config, settings ->
        GoogleAiGeminiChatModel.builder()
            .apiKey(config.apiKey ?: error("API key is required for Gemini!"))
            .modelName(config.modelName ?: settings?.model ?: settings?.deploymentName)
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
