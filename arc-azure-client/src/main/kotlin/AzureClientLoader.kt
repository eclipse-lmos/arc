// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.credential.KeyCredential
import com.azure.identity.DefaultAzureCredentialBuilder
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.CompleterLoaderService
import org.eclipse.lmos.arc.agents.llm.getEnvironmentValue
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory

class AzureClientLoader : CompleterLoaderService {

    private val log = LoggerFactory.getLogger(AzureClientLoader::class.java.name)

    private val clientNames = setOf("azure", "openai")

    override fun load(
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
        configs: List<AIClientConfig>,
    ): Map<String, ChatCompleter> = buildMap {
        configs.forEach { config ->
            if (clientNames.contains(config.client)) {
                putAll(loadAzure(config, tracer, eventPublisher))
            }
        }

        if (isEmpty()) {
            getEnvironmentValue("OPENAI_API_KEY")?.let { openAIApiKey ->
                log.info("[CLIENT] Using OPENAI_API_KEY to create Azure OpenAI client.")
                put(
                    ANY_MODEL,
                    AzureAIClient(
                        AIClientConfig(client = "openai"),
                        openAIClient(openAIApiKey),
                        eventPublisher,
                        tracer,
                    ),
                )
            }
        }
    }

    private fun loadAzure(
        config: AIClientConfig,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ) = buildMap {
        val azureClient = when {
            config.apiKey != null && config.endpoint == null -> openAIClient(config.apiKey!!)
            config.apiKey != null && config.endpoint != null -> azureClientWithKey(config.apiKey!!, config.endpoint!!)
            config.endpoint != null -> azureClient(config.endpoint!!)
            else -> null
        }

        if (azureClient != null) {
            put(
                config.modelAlias ?: config.modelName ?: ANY_MODEL,
                AzureAIClient(config, azureClient, eventPublisher, tracer),
            )
        }
    }

    private fun openAIClient(openAIApiKey: String): OpenAIAsyncClient {
        log.info("[CLIENT] Creating Azure OpenAI client with OpenAI API key.")
        return OpenAIClientBuilder()
            .credential(KeyCredential(openAIApiKey))
            .buildAsyncClient()
    }

    private fun azureClientWithKey(apiKey: String, endpoint: String): OpenAIAsyncClient {
        log.info("[CLIENT] Creating Azure OpenAI client with Azure API key and endpoint $endpoint.")
        return OpenAIClientBuilder()
            .endpoint(endpoint)
            .credential(AzureKeyCredential(apiKey))
            .buildAsyncClient()
    }

    private fun azureClient(endpoint: String): OpenAIAsyncClient {
        log.info("[CLIENT] Creating Azure OpenAI client with AzureCredentials and endpoint $endpoint.")
        return OpenAIClientBuilder()
            .credential(DefaultAzureCredentialBuilder().build())
            .endpoint(endpoint)
            .buildAsyncClient()
    }
}
