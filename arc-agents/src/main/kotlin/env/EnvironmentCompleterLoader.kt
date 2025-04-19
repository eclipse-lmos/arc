package org.eclipse.lmos.arc.agents.env

import org.eclipse.lmos.arc.agents.agent.AIClientConfig
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.toChatCompleterProvider
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileInputStream
import java.util.*

/**
 * Loads a [ChatCompleter] that is configured by environment variables.
 */
interface EnvironmentCompleterLoader {

    fun load(
        tracer: AgentTracer?,
        eventPublisher: EventPublisher?,
        configs: List<AIClientConfig>?,
    ): Map<String, ChatCompleter>
}

/**
 * An implementation of [ChatCompleterProvider] that loads [ChatCompleter]s from the environment.
 *
 */
class EnvironmentCompleterProvider : ChatCompleterProvider {

    private val log = LoggerFactory.getLogger(javaClass)

    private val completerProvider: ChatCompleterProvider by lazy {
        loadCompletersFromEnvironment()
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
    private fun loadCompletersFromEnvironment(
        tracer: AgentTracer? = null,
        eventPublisher: EventPublisher? = null,
    ): ChatCompleterProvider {
        val loader = ServiceLoader.load(EnvironmentCompleterLoader::class.java)
        return buildMap {
            loader.forEach {
                it.load(tracer, eventPublisher, null).forEach { (key, completer) ->
                    put(key, completer)
                    log.info("[CLIENT] Loaded ChatCompleter $key to $completer")
                }
            }
        }.toChatCompleterProvider()
    }
}

fun getEnvironmentValue(name: String): String? {
    return System.getenv(name) ?: System.getProperty(name) ?: loadArcProperties().getProperty(name)
}

private fun home(): File {
    val home = File(System.getProperty("user.home"), ".arc")
    home.mkdirs()
    return home
}

private fun loadArcProperties(): Properties {
    val properties = Properties()
    val propertiesFile = File(home(), "arc.properties")
    if (propertiesFile.exists()) {
        FileInputStream(propertiesFile).use { properties.load(it) }
    }
    return properties
}
