// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel
import dev.langchain4j.model.chat.ChatLanguageModel
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.client.langchain4j.LangChainConfig
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import java.util.concurrent.ConcurrentHashMap

private val cache = ConcurrentHashMap<Pair<LangChainConfig, ChatCompletionSettings?>, ChatLanguageModel>()

/**
 * Builds a BedrockAnthropicMessageChatModel for the given LangChainConfig and ChatCompletionSettings.
 */
fun bedrockBuilder(
    awsCredentialsProvider: AwsCredentialsProvider? = null,
): (LangChainConfig, ChatCompletionSettings?) -> ChatLanguageModel {
    return { model, settings ->
        cache.computeIfAbsent(model to settings) {
            BedrockAnthropicMessageChatModel.builder()
                .credentialsProvider(
                    awsCredentialsProvider ?: StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(model.accessKeyId, model.secretAccessKey),
                    ),
                )
                .region(Region.of(model.url))
                .model(model.modelName)
                .apply {
                    if (settings != null) {
                        settings.topP?.let { topP(it.toFloat()) }
                        settings.temperature?.let { temperature(it) }
                        settings.maxTokens?.let { maxTokens(it) }
                        settings.topK?.let { topK(it) }
                    }
                }
                .build()
        }
    }
}
