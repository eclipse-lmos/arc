// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import org.eclipse.lmos.arc.client.openai.OpenAINativeClient
import org.eclipse.lmos.arc.client.openai.OpenAINativeClientConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Configuration for the OpenAI client.
 */
@ConditionalOnClass(OpenAINativeClient::class)
class OpenAIConfiguration {

    @Bean
    fun openAiNativeClient() = ClientBuilder { config, eventPublisher ->
        if (config.client != "openai-sdk") return@ClientBuilder null
        val nativeClient = when {
            config.apiKey == null -> error("No API key provided for OpenAI Native client!")
            else -> {
                OpenAINativeClient(
                    OpenAINativeClientConfig(config.modelName, config.url ?: "", config.apiKey),
                    OpenAIOkHttpClientAsync.builder()
                        .apiKey(config.apiKey)
                        .build(),
                    eventPublisher,
                )
            }
        }
        nativeClient
    }
}
