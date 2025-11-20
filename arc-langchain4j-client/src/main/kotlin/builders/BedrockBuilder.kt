// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.bedrock.BedrockChatModel
import dev.langchain4j.model.bedrock.BedrockChatRequestParameters
import dev.langchain4j.model.chat.ChatModel
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient

/**
 * Builds a BedrockAnthropicMessageChatModel for the given LangChainConfig and ChatCompletionSettings.
 */
fun bedrockBuilder(
    awsCredentialsProvider: AwsCredentialsProvider? = null,
): (AIClientConfig, ChatCompletionSettings?) -> ChatModel {
    return { config, settings ->
        BedrockChatModel.builder()
            .client(
                BedrockRuntimeClient.builder()
                    .region(Region.of(config.region ?: "us-east-1"))
                    .credentialsProvider(
                        awsCredentialsProvider
                            ?: StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                    config.accessKey,
                                    config.accessSecret,
                                ),
                            ),
                    ).build(),
            )
            .region(Region.of(config.endpoint))
            .modelId(config.modelName ?: settings?.model ?: settings?.deploymentName)
            .defaultRequestParameters(
                BedrockChatRequestParameters.builder().apply {
                    settings?.temperature?.let { temperature(it) }
                    settings?.topP?.let { topP(it) }
                    settings?.maxTokens?.let { maxOutputTokens(it) }
                    settings?.topK?.let { topK(it) }
                }.build(),
            ).build()
    }
}
