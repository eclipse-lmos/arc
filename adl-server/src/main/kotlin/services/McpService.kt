// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.services

import org.eclipse.lmos.adl.server.models.McpServerDetails
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
import org.eclipse.lmos.arc.mcp.McpTools
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

/**
 * Service to manage Model Context Protocol (MCP) tools and connections.
 * It handles the lifecycle of McpTools for multiple server URLs.
 */
class McpService : Closeable, LLMFunctionLoader {
    private val mcpToolsMap = ConcurrentHashMap<String, McpTools>()

    /**
     * Sets the list of MCP server URLs to be used.
     * Verifies each URL by attempting to connect and load tools.
     *
     * @param urls The list of MCP server URLs.
     * @return A list of [McpServerDetails] objects containing status and tool count for each URL.
     */
    suspend fun setMcpServerUrls(urls: List<String>): List<McpServerDetails> {
        val serverStatuses = mutableListOf<McpServerDetails>()

        // Close and remove tools for URLs that are not in the new list
        mcpToolsMap.keys.toList().forEach { url ->
            if (!urls.contains(url)) {
                mcpToolsMap.remove(url)?.close()
            }
        }

        // Add or verify tools for URLs
        for (url in urls) {
            val tools = mcpToolsMap.getOrPut(url) { McpTools(url, Duration.ofMinutes(1)) }
            try {
                val loadedTools = tools.load(null)
                serverStatuses.add(McpServerDetails(url, true, loadedTools.size))
            } catch (_: Exception) {
                serverStatuses.add(McpServerDetails(url, false, 0))
            }
        }
        return serverStatuses
    }

    /**
     * Retrieves all tools from all registered MCP servers.
     *
     * @return A list of all available [LLMFunction]s.
     */
    suspend fun getAllTools(): List<LLMFunction> {
        return mcpToolsMap.values.flatMap {
            try {
                it.load(null)
            } catch (_: Exception) {
                emptyList()
            }
        }
    }

    /**
     * Returns the list of currently registered MCP server URLs.
     *
     * @return List of server URLs.
     */
    suspend fun getMcpServerUrls(): List<McpServerDetails> {
        return mcpToolsMap.entries.map { (url, tools) ->
            try {
                val loadedTools = tools.load(null)
                McpServerDetails(url, true, loadedTools.size)
            } catch (_: Exception) {
                McpServerDetails(url, false, 0)
            }
        }
    }

    /**
     * Closes all connections to MCP servers.
     */
    override fun close() {
        mcpToolsMap.values.forEach { it.close() }
        mcpToolsMap.clear()
    }

    /**
     * Loads functions/tools for the agent execution context.
     *
     * @param context The context for tool loading (optional).
     * @return A list of loaded [LLMFunction]s.
     */
    override suspend fun load(context: ToolLoaderContext?): List<LLMFunction> {
        return getAllTools()
    }
}
