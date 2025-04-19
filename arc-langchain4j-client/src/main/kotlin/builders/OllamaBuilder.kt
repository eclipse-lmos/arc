// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.chat.ChatLanguageModel
import dev.langchain4j.model.ollama.OllamaChatModel
import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings

/**
 * Builds a ollama client.
 */
fun ollamaBuilder(): (AIClientConfig, ChatCompletionSettings?) -> ChatLanguageModel {
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
