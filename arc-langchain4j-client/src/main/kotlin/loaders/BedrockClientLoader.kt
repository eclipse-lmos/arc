// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j.loaders

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.bedrockBuilder

class BedrockClientLoader : ClientLoader(
    name = "BEDROCK",
    dependOnClass = "dev.langchain4j.model.bedrock.BedrockAnthropicMessageChatModel",
    clientNames = setOf("bedrock"),
) {

    override fun loadClient(
        config: AIClientConfig,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ) = buildMap {
        config.accessKey ?: error("AccessKey is required for bedrock!")
        config.accessSecret ?: error("AccessSecret is required for bedrock!")
        config.endpoint ?: error("Endpoint is required for bedrock!")
        val client = LangChainClient(
            AIClientConfig(
                modelName = config.modelName,
                endpoint = config.endpoint,
                accessKey = config.accessKey,
                accessSecret = config.accessSecret,
            ),
            bedrockBuilder(),
            eventPublisher,
        )
        put(config.id ?: config.modelName ?: ANY_MODEL, client)
    }
}
