// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import dev.langchain4j.model.ollama.OllamaChatModel
import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.ollamaBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Configuration for the Langchain4j ollama client.
 */
@ConditionalOnClass(OllamaChatModel::class)
class OllamaConfiguration {

    @Bean
    fun ollamaClient() = ClientBuilder { config, eventPublisher ->
        if (config.client != "ollama") return@ClientBuilder null
        LangChainClient(
            AIClientConfig(modelName = config.modelName, endpoint = config.url),
            ollamaBuilder(),
            eventPublisher,
        )
    }
}
