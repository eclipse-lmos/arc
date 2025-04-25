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
        configs: List<AIClientConfig>,
    ): Map<String, ChatCompleter>
}

/**
 * An implementation of [ChatCompleterProvider] that loads [ChatCompleter]s discovered using the ServiceLoader.
 *
 */
class ServiceCompleterProvider(
    private val configs: List<AIClientConfig>? = null,
    private val tracer: AgentTracer? = null,
    private val eventPublisher: EventPublisher? = null,
) : ChatCompleterProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    private val completerProvider: ChatCompleterProvider by lazy {
        loadCompletersProviders()
    }

    override fun provideByModel(model: String?): ChatCompleter {
        return completerProvider.provideByModel(model)
    }

    /**
     * Loads [ChatCompleter]s using the ServiceLoader.
     */
    private fun loadCompletersProviders(): ChatCompleterProvider {
        val loader = ServiceLoader.load(CompleterLoaderService::class.java)
        val loadedConfigs = configs ?: loadConfigFromEnv()
        return buildMap {
            loader.forEach {
                it.load(tracer, eventPublisher, loadedConfigs).forEach { (key, completer) ->
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
            } else {
                log.info("[CLIENT] Found $size ChatCompleters!")
            }
        }.toChatCompleterProvider()
    }

    override fun toString() =
        "ServiceCompleterProvider(completerProvider=$completerProvider, configs=$configs, tracer=$tracer, eventPublisher=$eventPublisher)"
}

fun getEnvironmentValue(name: String): String? {
    return getProperty(name) ?: getenv(name) ?: loadArcProperties().getProperty(name)
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

fun loadConfigFromEnv(): List<AIClientConfig> = buildList {
    getEnvironmentValue("ARC_CLIENT")?.let { client ->
        val modelAlias = getEnvironmentValue("ARC_MODEL_ALIAS")
        val modelName = getEnvironmentValue("ARC_MODEL")
        val endpoint = getEnvironmentValue("ARC_AI_URL")
        val apiKey = getEnvironmentValue("ARC_AI_KEY")
        val accessKey = getEnvironmentValue("ARC_AI_ACCESS_KEY")
        val accessSecret = getEnvironmentValue("ARC_AI_ACCESS_SECRET")
        add(
            AIClientConfig(
                modelAlias = modelAlias,
                modelName = modelName,
                endpoint = endpoint,
                apiKey = apiKey,
                client = client,
                accessKey = accessKey,
                accessSecret = accessSecret,
            ),
        )
    }

    repeat(10) { i ->
        getEnvironmentValue("ARC_CLIENT[$i]")?.let { client ->
            val modelAlias = getEnvironmentValue("ARC_MODEL_ALIAS[$i]")
            val modelName = getEnvironmentValue("ARC_MODEL[$i]")
            val endpoint = getEnvironmentValue("ARC_AI_URL[$i]")
            val apiKey = getEnvironmentValue("ARC_AI_KEY[$i]")
            val accessKey = getEnvironmentValue("ARC_AI_ACCESS_KEY[$i]")
            val accessSecret = getEnvironmentValue("ARC_AI_ACCESS_SECRET[$i]")
            add(
                AIClientConfig(
                    modelAlias = modelAlias,
                    modelName = modelName,
                    endpoint = endpoint,
                    apiKey = apiKey,
                    client = client,
                    accessKey = accessKey,
                    accessSecret = accessSecret,
                ),
            )
        }
    }
}
