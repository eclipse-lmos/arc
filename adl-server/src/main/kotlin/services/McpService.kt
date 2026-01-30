// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.services

import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.mcp.McpTools
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

class McpService : Closeable {
    private val mcpToolsMap = ConcurrentHashMap<String, McpTools>()

    fun setMcpServerUrls(urls: List<String>) {
        // Close and remove tools for URLs that are not in the new list
        mcpToolsMap.keys.toList().forEach { url ->
            if (!urls.contains(url)) {
                mcpToolsMap.remove(url)?.close()
            }
        }

        // Add new tools for URLs that are not in the map
        urls.forEach { url ->
            if (!mcpToolsMap.containsKey(url)) {
                try {
                    mcpToolsMap[url] = McpTools(url, Duration.ofMinutes(5))
                } catch (e: Exception) {
                    // Handle connection error or log it.
                    // For now, we might want to skip or throw, but let's just log print stack trace in this mocked simplified env
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun getAllTools(): List<LLMFunction> {
        return mcpToolsMap.values.flatMap { it.load(null) }
    }

    fun getMcpServerUrls(): List<String> {
        return mcpToolsMap.keys.toList()
    }

    override fun close() {
        mcpToolsMap.values.forEach { it.close() }
        mcpToolsMap.clear()
    }
}
