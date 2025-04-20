// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ServiceCompleterProvider
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.spring.AIConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@ConditionalOnMissingBean(ChatCompleterProvider::class)
@EnableConfigurationProperties(AIConfig::class)
class ClientsConfiguration {

    @Bean
    fun chatCompleterProvider(
        aiConfig: AIConfig,
        eventPublisher: EventPublisher?,
        tracer: AgentTracer?,
    ): ChatCompleterProvider {
        return ServiceCompleterProvider(
            aiConfig.clients.map { config ->
                org.eclipse.lmos.arc.agents.llm.AIClientConfig(
                    modelName = config.modelName,
                    endpoint = config.url,
                    accessKey = config.accessKey,
                    accessSecret = config.accessSecret,
                    apiKey = config.apiKey,
                    modelAlias = config.id,
                    client = config.client,
                )
            },
            tracer,
            eventPublisher,
        )
    }
}
