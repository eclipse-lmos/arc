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

class AzureClientLoader : EnvironmentCompleterLoader {

    override fun loadCompleter(
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
            apiKey != null -> azureClientWithKey(apiKey, endpoint)
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
        return OpenAIClientBuilder()
            .credential(KeyCredential(openAIApiKey))
            .buildAsyncClient()
    }

    private fun azureClientWithKey(apiKey: String, endpoint: String?): OpenAIAsyncClient {
        return OpenAIClientBuilder()
            .apply {
                if (endpoint != null) endpoint(endpoint)
            }
            .credential(AzureKeyCredential(apiKey))
            .buildAsyncClient()
    }

    private fun azureClient(endpoint: String): OpenAIAsyncClient {
        return OpenAIClientBuilder()
            .credential(DefaultAzureCredentialBuilder().build())
            .endpoint(endpoint)
            .buildAsyncClient()
    }
}
