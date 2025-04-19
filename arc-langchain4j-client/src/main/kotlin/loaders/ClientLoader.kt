// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.client.langchain4j.loaders

import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.agents.env.EnvironmentCompleterLoader
import org.eclipse.lmos.arc.agents.env.getEnvironmentValue
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory

abstract class ClientLoader(
    private val name: String,
    private val dependOnClass: String,
    private val clientNames: Set<String>,
) :
    EnvironmentCompleterLoader {

    private val log = LoggerFactory.getLogger(ClientLoader::class.java)

    override fun load(
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
        configs: List<AIClientConfig>?,
    ): Map<String, ChatCompleter> = buildMap {
        val classLoader = Thread.currentThread().contextClassLoader

        try {
            classLoader.loadClass(dependOnClass)
        } catch (ex: ClassNotFoundException) {
            log.info("$name library not found, skipping $name client loading.")
            return@buildMap
        }

        if (configs != null) {
            configs.forEach { config ->
                if (clientNames.contains(config.client)) {
                    putAll(loadClient(config, tracer, eventPublisher))
                }
            }
            if (isNotEmpty()) return@buildMap
        }

        getEnvironmentValue("ARC_${name}_MODEL_NAME")?.let { modelName ->
            putAll(
                loadClient(
                    AIClientConfig(
                        modelName = modelName,
                        endpoint = getEnvironmentValue("ARC_${name}_ENDPOINT"),
                        apiKey = getEnvironmentValue("ARC_${name}_API_KEY")
                    ),
                    tracer,
                    eventPublisher,
                ),
            )
        }

        repeat(10) { i ->
            getEnvironmentValue("ARC_$name[$i]_MODEL_NAME")?.let { modelName ->
                val endpoint = getEnvironmentValue("ARC_$name[$i]_ENDPOINT")
                val apiKey = getEnvironmentValue("ARC_$name[$i]_API_KEY")
                putAll(
                    loadClient(
                        AIClientConfig(modelName = modelName, endpoint = endpoint, apiKey = apiKey),
                        tracer,
                        eventPublisher
                    )
                )
            }
        }

        // Handle legacy properties
        getEnvironmentValue("ARC_CLIENT")?.takeIf { clientNames.contains(it) }?.let {
            val modelName = getEnvironmentValue("ARC_MODEL") ?: error("Missing property ARC_MODEL!")
            val endpoint = getEnvironmentValue("ARC_AI_URL")
            val apiKey = getEnvironmentValue("ARC_AI_KEY")
            putAll(
                loadClient(
                    AIClientConfig(modelName = modelName, endpoint = endpoint, apiKey = apiKey),
                    tracer,
                    eventPublisher
                )
            )
        }
    }

    abstract fun loadClient(
        config: AIClientConfig,
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
    ): Map<String, ChatCompleter>
}
