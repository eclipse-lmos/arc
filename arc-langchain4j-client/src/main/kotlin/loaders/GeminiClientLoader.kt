// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j.loaders

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.client.langchain4j.LangChainClient
import org.eclipse.lmos.arc.client.langchain4j.builders.geminiBuilder

class GeminiClientLoader : ClientLoader(
    name = "GEMINI",
    dependOnClass = "dev.langchain4j.model.googleai.GoogleAiGeminiChatModel",
    clientNames = setOf("gemini"),
) {

    override fun loadClient(
        config: AIClientConfig,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ) = buildMap {
        config.apiKey ?: error("API key is required for Gemini!")
        val client = LangChainClient(config, geminiBuilder(), eventPublisher, tracer)
        put(config.modelAlias ?: config.modelName ?: ANY_MODEL, client)
    }
}
