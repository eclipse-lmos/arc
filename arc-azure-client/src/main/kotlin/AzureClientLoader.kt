// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.azure

import com.azure.ai.openai.OpenAIAsyncClient
import com.azure.ai.openai.OpenAIClientBuilder
import com.azure.core.credential.AzureKeyCredential
import com.azure.core.credential.KeyCredential
import com.azure.identity.DefaultAzureCredentialBuilder
import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.agents.env.EnvironmentCompleterLoader
import org.eclipse.lmos.arc.agents.env.getEnvironmentValue
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class AzureClientLoader : EnvironmentCompleterLoader {

    private val log = LoggerFactory.getLogger(AzureClientLoader::class.java.name)

    override fun load(
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
        configs: List<AIClientConfig>?,
    ): Map<String, ChatCompleter> = buildMap {
        putAll(
            loadAzure(
                getEnvironmentValue("ARC_AZURE_MODEL_NAME"),
                getEnvironmentValue("ARC_AZURE_ENDPOINT"),
                getEnvironmentValue("ARC_AZURE_API_KEY"),
                tracer,
                eventPublisher,
            ),
        )

        repeat(10) { i ->
            getEnvironmentValue("ARC_AZURE[$i]_MODEL_NAME")?.let { modelName ->
                val endpoint = getEnvironmentValue("ARC_AZURE[$i]_ENDPOINT")
                val apiKey = getEnvironmentValue("ARC_AZURE[$i]_API_KEY")
                putAll(loadAzure(modelName, endpoint, apiKey, tracer, eventPublisher))
            }
        }

        // Handle legacy properties
        getEnvironmentValue("ARC_CLIENT")?.takeIf { it == "azure" || it == "openai" }?.let {
            val modelName = getEnvironmentValue("ARC_MODEL")
            val endpoint = getEnvironmentValue("ARC_AI_URL")
            val apiKey = getEnvironmentValue("ARC_AI_KEY")
            putAll(loadAzure(modelName, endpoint, apiKey, tracer, eventPublisher))
        }

        // Ignore the OpenAI API key if we already have an Azure Client defined that has no default model name.
        putAll(loadOpenAI(tracer, eventPublisher, useOpenAIKey = get(ANY_MODEL) == null))
    }

    private fun loadOpenAI(tracer: AgentTracer?, eventPublisher: EventPublisher?, useOpenAIKey: Boolean) =
        buildMap<String, ChatCompleter> {
            getEnvironmentValue("ARC_OPENAI_API_KEY")?.let { openAIApiKey ->
                put(
                    ANY_MODEL, AzureAIClient(AzureClientConfig(), openAIClient(openAIApiKey), eventPublisher, tracer),
                )
            } ?: getEnvironmentValue("OPENAI_API_KEY")?.let { openAIApiKey ->
                if (useOpenAIKey) {
                    log.info("[CLIENT] Using OPENAI_API_KEY to create Azure OpenAI client.")
                    put(
                        ANY_MODEL,
                        AzureAIClient(AzureClientConfig(), openAIClient(openAIApiKey), eventPublisher, tracer),
                    )
                }
            }
        }

    private fun loadAzure(
        modelName: String?,
        endpoint: String?,
        apiKey: String?,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ) = buildMap {
        val azureClient = when {
            apiKey != null && endpoint == null -> openAIClient(apiKey)
            apiKey != null && endpoint != null -> azureClientWithKey(apiKey, endpoint)
            endpoint != null -> azureClient(endpoint)
            else -> null
        }

        if (azureClient != null) {
            put(
                modelName ?: ANY_MODEL,
                AzureAIClient(
                    AzureClientConfig(modelName),
                    azureClient,
                    eventPublisher,
                    tracer,
                ),
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
