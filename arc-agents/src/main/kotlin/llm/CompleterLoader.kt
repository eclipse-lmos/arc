// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.agents.llm

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.lang.System.getProperty
import java.lang.System.getenv
import java.util.*

/**
 * Loads [ChatCompleter]s through services discovered using the ServiceLoader.
 */
interface CompleterLoaderService {

    fun load(
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
        configs: List<AIClientConfig>?,
    ): Map<String, ChatCompleter>
}

/**
 * An implementation of [ChatCompleterProvider] that loads [ChatCompleter]s discovered using the ServiceLoader.
 *
 */
class ServiceCompleterProvider(private val configs: List<AIClientConfig>? = null) : ChatCompleterProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    private val completerProvider: ChatCompleterProvider by lazy {
        loadCompletersProviders()
    }

    override fun provideByModel(model: String?): ChatCompleter {
        return completerProvider.provideByModel(model)
    }

    /**
     * Loads all [ChatCompleter]s from the environment.
     *
     * @param tracer the tracer to use
     * @param eventPublisher the event publisher to use
     * @return a [ChatCompleterProvider] that loads [ChatCompleter]s from the environment
     */
    private fun loadCompletersProviders(
        tracer: AgentTracer? = null,
        eventPublisher: EventPublisher? = null,
    ): ChatCompleterProvider {
        val loader = ServiceLoader.load(CompleterLoaderService::class.java)
        return buildMap {
            loader.forEach {
                it.load(tracer, eventPublisher, configs).forEach { (key, completer) ->
                    put(key, completer)
                    log.info("[CLIENT] Loaded ChatCompleter $key to $completer")
                }
            }
            if (isEmpty()) {
                log.warn("[CLIENT] No ChatCompleters found!")
            } else {
                log.info("[CLIENT] Found $size ChatCompleters!")
            }
        }.toChatCompleterProvider()
    }
}

fun getEnvironmentValue(name: String): String? {
    return getenv(name) ?: getProperty(name) ?: loadArcProperties().getProperty(name)
}

private fun home(): File {
    val home = File(getProperty("user.home"), ".arc")
    home.mkdirs()
    return home
}

private fun loadArcProperties(): Properties {
    if ((getenv("ARC_IGNORE_ARC_PROPERTIES") ?: getProperty("ARC_IGNORE_ARC_PROPERTIES")) == "true") {
        return Properties()
    }
    val properties = Properties()
    val propertiesFile = File(home(), "arc.properties")
    if (propertiesFile.exists()) {
        FileInputStream(propertiesFile).use { properties.load(it) }
    }
    return properties
}
