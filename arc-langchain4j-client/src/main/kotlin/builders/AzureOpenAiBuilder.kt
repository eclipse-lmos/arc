// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import com.azure.ai.openai.models.ReasoningEffortValue
import dev.langchain4j.model.azure.AzureOpenAiChatModel
import dev.langchain4j.model.chat.ChatModel
import dev.langchain4j.model.chat.request.ResponseFormat
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.OutputFormat
import org.eclipse.lmos.arc.agents.llm.ReasoningEffort

/**
 * Builds an Azure OpenAI client.
 *
 * LangChain4j's Azure client does not currently expose Azure's service-tier option.
 */
fun azureOpenAiBuilder(): (AIClientConfig, ChatCompletionSettings?) -> ChatModel {
    return { config, settings ->
        AzureOpenAiChatModel.builder()
            .endpoint(config.endpoint ?: error("Model endpoint is required for Azure OpenAI!"))
            .apiKey(config.apiKey ?: error("API key is required for Azure OpenAI!"))
            .deploymentName(config.modelName ?: settings?.deploymentNameOrModel())
            .apply {
                settings?.temperature?.let { temperature(it) }
                settings?.topP?.let { topP(it) }
                settings?.seed?.let { seed(it) }
                settings?.maxTokens?.let { maxTokens(it) }
                settings?.reasoningEffort?.let {
                    reasoningEffort(
                        when (it) {
                            ReasoningEffort.LOW -> ReasoningEffortValue.LOW
                            ReasoningEffort.MEDIUM -> ReasoningEffortValue.MEDIUM
                            ReasoningEffort.HIGH -> ReasoningEffortValue.HIGH
                        },
                    )
                }
                settings?.format?.let {
                    responseFormat(
                        when (it) {
                            OutputFormat.TEXT -> ResponseFormat.TEXT
                            OutputFormat.JSON -> ResponseFormat.JSON
                        },
                    )
                }
            }
            .build()
    }
}

