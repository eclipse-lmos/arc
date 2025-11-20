// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.ollama.OllamaChatModel
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings

/**
 * Builds an ollama client.
 */
fun ollamaBuilder(): (AIClientConfig, ChatCompletionSettings?) -> ChatModel {
    return { config, settings ->
        OllamaChatModel
            .builder()
            .baseUrl(config.endpoint ?: "http://localhost:11434")
            .modelName(config.modelName ?: settings?.model ?: settings?.deploymentName)
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
