// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel
import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.geminiBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Configuration for the Langchain4j gemini client.
 */
@ConditionalOnClass(GoogleAiGeminiChatModel::class)
class GeminiConfiguration {

    @Bean
    fun geminiClient() = ClientBuilder { config, eventPublisher ->
        if (config.client != "gemini") return@ClientBuilder null
        LangChainClient(
            AIClientConfig(
                modelName = config.modelName,
                endpoint = config.url,
                apiKey = config.apiKey,
            ),
            geminiBuilder(),
            eventPublisher,
        )
    }
}
