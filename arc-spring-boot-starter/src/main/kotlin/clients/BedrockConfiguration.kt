// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring.clients

import dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel
import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.bedrockBuilder
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider

/**
 * Configuration for the Langchain4j bedrock client.
 */
@ConditionalOnClass(BedrockAnthropicMessageChatModel::class)
class BedrockConfiguration {

    @Bean
    fun bedrockClient(awsCredentialsProvider: AwsCredentialsProvider? = null) =
        ClientBuilder { config, eventPublisher ->
            if (config.client != "bedrock") return@ClientBuilder null
            LangChainClient(
                AIClientConfig(
                    modelName = config.modelName,
                    endpoint = config.url,
                    accessKey = config.accessKey,
                    accessSecret = config.accessSecret,
                    apiKey = null,
                ),
                bedrockBuilder(awsCredentialsProvider),
                eventPublisher,
            )
        }
}
