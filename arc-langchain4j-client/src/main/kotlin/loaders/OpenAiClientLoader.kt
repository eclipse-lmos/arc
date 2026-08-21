// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j.loaders

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.openAiBuilder

class OpenAiClientLoader : ClientLoader(
    name = "OPENAI_RESPONSES",
    dependOnClass = "dev.langchain4j.model.openai.OpenAiResponsesChatModel",
    clientNames = setOf("langchain4j-openai", "langchain4j-openai-responses"),
) {

    override fun loadClient(
        config: AIClientConfig,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ) = buildMap {
        config.apiKey ?: error("API key is required for OpenAI!")
        val client = LangChainClient(config, openAiBuilder(), eventPublisher, tracer)
        put(config.modelAlias ?: config.modelName ?: ANY_MODEL, client)
    }
}

