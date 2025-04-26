// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
import org.slf4j.LoggerFactory
import java.lang.System.getProperty
import java.lang.System.getenv
import java.time.Duration

/**
 * Loads instances of [McpTools] from environment variables.
 * This class is set up to be discovered by the [LLMFunctionServiceLoader].
 */
class McpToolsByEnvironment : LLMFunctionLoader {

    private val log = LoggerFactory.getLogger(javaClass)

    private val tools: List<McpTools> by lazy {
        val urls = getEnvironmentValue("ARC_MCP_TOOLS_URLS")?.split(",") ?: return@lazy emptyList()
        val cacheDuration = getEnvironmentValue("ARC_MCP_TOOLS_CACHE_DURATION")?.let { Duration.parse(it) }
        log.info("Loading MCP tools from URLs: $urls")
        urls.map { url ->
            McpTools(url.trim(), cacheDuration)
        }
    }

    override suspend fun load(context: ToolLoaderContext?): List<LLMFunction> {
        return tools.flatMap { it.load(context) }
    }

    private fun getEnvironmentValue(name: String): String? {
        return getProperty(name) ?: getenv(name)
    }
}
