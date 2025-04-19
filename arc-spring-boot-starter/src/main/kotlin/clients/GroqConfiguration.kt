// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import dev.langchain4j.model.openai.OpenAiChatModel
import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.groqBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Configuration for Langchain4j based LLM clients.
 */
@ConditionalOnClass(dev.langchain4j.model.openai.OpenAiChatModel::class)
class GroqConfiguration {

    @Bean
    @ConditionalOnClass(OpenAiChatModel::class)
    fun groqClient() = ClientBuilder { config, eventPublisher ->
        if (config.client != "groq") return@ClientBuilder null
        LangChainClient(
            AIClientConfig(
                modelName = config.modelName,
                endpoint = config.url,
                apiKey = config.apiKey,
            ),
            groqBuilder(),
            eventPublisher,
        )
    }
}
