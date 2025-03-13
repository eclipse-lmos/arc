// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.credential.KeyCredential
import com.azure.identity.DefaultAzureCredentialBuilder
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.client.azure.AzureAIClient
import org.eclipse.lmos.arc.client.azure.AzureClientConfig
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

/**
 * Configuration for the Azure/OpenAI client.
 */
@ConditionalOnClass(OpenAIAsyncClient::class)
class AzureOpenAIConfiguration {

    /**
     * The Azure OpenAI client setup to connect to the OpenAI API or Azure OpenAI.
     * See https://github.com/Azure/azure-sdk-for-java/tree/main/sdk/openai/azure-ai-openai#support-for-non-azure-openai
     */
    @Bean
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
    @ConditionalOnClass(DefaultAzureCredentialBuilder::class)
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
}
