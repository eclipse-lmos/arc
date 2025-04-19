// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j.loaders

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.CompleterLoaderService
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory

abstract class ClientLoader(
    private val name: String,
    private val dependOnClass: String,
    private val clientNames: Set<String>,
) :
    CompleterLoaderService {

    private val log = LoggerFactory.getLogger(ClientLoader::class.java)

    override fun load(
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
        configs: List<AIClientConfig>,
    ): Map<String, ChatCompleter> = buildMap {
        val classLoader = Thread.currentThread().contextClassLoader

        try {
            classLoader.loadClass(dependOnClass)
        } catch (ex: ClassNotFoundException) {
            log.info("$name library not found, skipping $name client loading.")
            return@buildMap
        }

        configs.forEach { config ->
            if (clientNames.contains(config.client)) {
                putAll(loadClient(config, tracer, eventPublisher))
            }
        }
    }

    abstract fun loadClient(
        config: AIClientConfig,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ): Map<String, ChatCompleter>
}
