// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.client.langchain4j.builders

import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel
import dev.langchain4j.model.chat.ChatLanguageModel
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import java.util.concurrent.atomic.AtomicReference

val globalAwsCredentialsProvider = AtomicReference<Any>()

/**
 * Builds a BedrockAnthropicMessageChatModel for the given LangChainConfig and ChatCompletionSettings.
 */
fun bedrockBuilder(
    awsCredentialsProvider: AwsCredentialsProvider? = null,
): (AIClientConfig, ChatCompletionSettings?) -> ChatLanguageModel {
    return { config, settings ->
        BedrockAnthropicMessageChatModel.builder()
            .credentialsProvider(
                awsCredentialsProvider ?: globalAwsCredentialsProvider.get()?.takeIf { it is AwsCredentialsProvider }
                    ?.let { it as AwsCredentialsProvider }
                    ?: StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(config.accessKey, config.accessSecret),
                    ),
            )
            .region(Region.of(config.endpoint))
            .model(config.modelName ?: settings?.model ?: settings?.deploymentName)
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
