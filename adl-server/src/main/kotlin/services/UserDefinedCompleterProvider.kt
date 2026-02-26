// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.adl.server.services

import org.eclipse.lmos.adl.server.models.UserSettings
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.llm.ANY_MODEL
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.CompleterLoaderService
import org.eclipse.lmos.arc.agents.llm.ServiceCompleterProvider
import org.eclipse.lmos.arc.agents.llm.toChatCompleterProvider
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory
import java.util.ServiceLoader
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.component1
import kotlin.collections.component2

class UserDefinedCompleterProvider(
    private val eventPublisher: EventPublisher? = null,
    private val delegate: ChatCompleterProvider = ServiceCompleterProvider(eventPublisher = eventPublisher),
    private val tracer: AgentTracer? = null,
) : ChatCompleterProvider {

    private val log = LoggerFactory.getLogger(javaClass)
    private val userCompleter = AtomicReference<ChatCompleterProvider?>()

    fun updateSettings(settings: UserSettings) {
        val config = AIClientConfig(
            modelName = settings.modelName,
            client = "openai",
            apiKey = settings.apiKey,
            endpoint = settings.modelUrl
        )
        val completer = loadCompletersProviders(config)
        userCompleter.set(completer)
    }

    private fun loadCompletersProviders(config: AIClientConfig): ChatCompleterProvider? {
        val loader = ServiceLoader.load(CompleterLoaderService::class.java)
        return buildMap {
            loader.forEach {
                it.load(tracer, eventPublisher, listOf(config)).forEach { (key, completer) ->
                    if (containsKey(key)) {
                        error(
                            "Cannot have multiple ChatCompleters for the same key $key! Found: $completer and ${
                                get(
                                    ANY_MODEL,
                                )
                            }",
                        )
                    }
                    put(key, completer)
                    log.info("[CLIENT] Loaded ChatCompleter $key to $completer")
                }
            }
            if (isEmpty()) {
                log.warn("[CLIENT] No ChatCompleters found!")
                return null
            } else {
                log.info("[CLIENT] Found $size ChatCompleters!")
            }
        }.toChatCompleterProvider()
    }

    override fun provideByModel(model: String?): ChatCompleter {
        return userCompleter.get()?.provideByModel(model) ?: delegate.provideByModel(model)
    }
}

