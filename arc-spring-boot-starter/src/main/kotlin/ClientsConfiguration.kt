// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.credential.KeyCredential
import com.azure.identity.DefaultAzureCredentialBuilder
import com.openai.client.okhttp.OpenAIOkHttpClientAsync
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.client.azure.AzureAIClient
import org.eclipse.lmos.arc.client.azure.AzureClientConfig
import org.eclipse.lmos.arc.client.ollama.OllamaClient
import org.eclipse.lmos.arc.client.ollama.OllamaClientConfig
import org.eclipse.lmos.arc.client.openai.OpenAINativeClient
import org.eclipse.lmos.arc.client.openai.OpenAINativeClientConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean

@ConditionalOnMissingBean(ChatCompleterProvider::class)
@EnableConfigurationProperties(AIConfig::class)
class ClientsConfiguration {

    /**
     * The Azure OpenAI client setup to connect to the OpenAI API or Azure OpenAI.
     * See https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/openai/azure-ai-openai#support-for-non-azure-openai
     */
    @Bean
    @ConditionalOnClass(OpenAIAsyncClient::class)
    fun openAIAsyncClient(tracer: AgentTracer? = null) = ClientBuilder { config, eventPublisher ->
        if (config.client != "azure" && config.client != "openai") return@ClientBuilder null
        val azureClient = when {
            config.client == "openai" || config.url == null -> OpenAIClientBuilder()
                .apply { if (config.url != null) endpoint(config.url) }
                .credential(KeyCredential(config.apiKey))
                .buildAsyncClient()

            config.apiKey != null -> OpenAIClientBuilder()
                .endpoint(config.url)
                .credential(AzureKeyCredential(config.apiKey))
                .buildAsyncClient()

            else -> return@ClientBuilder null
        }
        AzureAIClient(
            AzureClientConfig(config.modelName, config.url ?: "", config.apiKey ?: ""),
            azureClient,
            eventPublisher,
            tracer,
        )
    }

    /**
     * The Azure OpenAI client setup to connect to the Azure OpenAI using Azure Credentials.
     */
    @Bean
    @ConditionalOnClass(OpenAIAsyncClient::class, DefaultAzureCredentialBuilder::class)
    fun openAIAsyncClientWithAzureCredentials(tracer: AgentTracer? = null) = ClientBuilder { config, eventPublisher ->
        if (config.client != "azure") return@ClientBuilder null
        val azureClient = when {
            config.url != null && config.apiKey == null -> OpenAIClientBuilder()
                .credential(DefaultAzureCredentialBuilder().build())
                .endpoint(config.url)
                .buildAsyncClient()

            else -> return@ClientBuilder null
        }
        AzureAIClient(
            AzureClientConfig(config.modelName, config.url ?: "", config.apiKey ?: ""),
            azureClient,
            eventPublisher,
            tracer,
        )
    }

    @Bean
    @ConditionalOnClass(OllamaClient::class)
    private fun ollamaClient() = ClientBuilder { config, eventPublisher ->
        if (config.client != "ollama") return@ClientBuilder null
        OllamaClient(OllamaClientConfig(config.modelName, config.url, config.toolSupported), eventPublisher)
    }

    @Bean
    @ConditionalOnClass(OpenAINativeClient::class)
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

    @Bean
    fun chatCompleterProvider(
        aiConfig: AIConfig,
        clientBuilders: List<ClientBuilder>,
        eventPublisher: EventPublisher?,
    ): ChatCompleterProvider {
        val clients = aiConfig.clients.associate { config ->
            config.id to (
                clientBuilders.firstNotNullOfOrNull { it.build(config, eventPublisher) }
                    ?: error("Cannot build client for $config!")
                )
        }
        return ChatCompleterProvider { model -> model?.let { clients[it] } ?: clients.values.first() }
    }
}

fun interface ClientBuilder {
    fun build(model: AIClientConfig, eventPublisher: EventPublisher?): ChatCompleter?
}
