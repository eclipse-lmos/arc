// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.toChatCompleterProvider
import org.eclipse.lmos.arc.spring.AIClientConfig
import org.eclipse.lmos.arc.spring.AIConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import

@ConditionalOnMissingBean(ChatCompleterProvider::class)
@EnableConfigurationProperties(AIConfig::class)
@Import(
    BedrockConfiguration::class,
    GeminiConfiguration::class,
    OpenAIConfiguration::class,
    AzureOpenAIConfiguration::class,
    GroqConfiguration::class,
    OllamaConfiguration::class,
)
class ClientsConfiguration {

    @Bean
    fun chatCompleterProvider(
        aiConfig: AIConfig,
        clientBuilders: List<ClientBuilder>,
        eventPublisher: EventPublisher?,
    ): ChatCompleterProvider {
        return aiConfig.clients.associate { config ->
            config.id to (
                clientBuilders.firstNotNullOfOrNull { it.build(config, eventPublisher) }
                    ?: error("Cannot build client for $config!")
                )
        }.toChatCompleterProvider()
    }
}

fun interface ClientBuilder {
    fun build(model: AIClientConfig, eventPublisher: EventPublisher?): ChatCompleter?
}
