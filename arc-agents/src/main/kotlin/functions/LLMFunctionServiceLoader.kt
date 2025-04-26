// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.functions

import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.llm.AIClientConfig
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.slf4j.LoggerFactory
import java.util.*

/**
 * An implementation of [LLMFunctionLoader] that uses Java's [ServiceLoader] to discover and load
 * other [LLMFunctionLoader] implementations from the classpath.
 *
 * This class acts as an aggregator of LLM functions by delegating to all discovered function loaders.
 * When the [load] method is called, it collects and returns all functions from all discovered loaders.
 */
class LLMFunctionServiceLoader(
    /**
     * Optional list of AI client configurations that may be used by the loaded function loaders.
     */
    private val configs: List<AIClientConfig>? = null,

    /**
     * Optional tracer for tracking function loader operations.
     */
    private val tracer: AgentTracer? = null,

    /**
     * Optional event publisher for publishing events related to function loading.
     */
    private val eventPublisher: EventPublisher? = null,
) : LLMFunctionLoader {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Lazily loaded list of [LLMFunctionLoader] implementations discovered via [ServiceLoader].
     */
    private val functionLoaders: List<LLMFunctionLoader> by lazy {
        loadFunctionLoaders()
    }

    /**
     * Loads all LLM functions by delegating to all discovered [LLMFunctionLoader] implementations.
     *
     * @param context Optional context that may be used by the function loaders to customize function loading.
     * @return A list of all [LLMFunction]s loaded from all discovered loaders.
     */
    override suspend fun load(context: ToolLoaderContext?): List<LLMFunction> {
        return functionLoaders.flatMap { it.load(context) }
    }

    /**
     * Uses Java's [ServiceLoader] to discover and load all implementations of [LLMFunctionLoader]
     * available on the classpath.
     *
     * @return A list of discovered [LLMFunctionLoader] implementations.
     */
    private fun loadFunctionLoaders(): List<LLMFunctionLoader> {
        val loaders = ServiceLoader.load(LLMFunctionLoader::class.java)
        return loaders.toList().also {
            log.info("Loaded ${it.size} LLMFunctionLoader(s): ${it.joinToString(", ") { it::class.simpleName.toString() }}")
        }
    }

    /**
     * Returns a string representation of this [LLMFunctionServiceLoader] including all discovered function loaders.
     *
     * @return A string representation of this object.
     */
    override fun toString() = "LLMFunctionServiceLoader(functionLoaders=$functionLoaders)"
}
