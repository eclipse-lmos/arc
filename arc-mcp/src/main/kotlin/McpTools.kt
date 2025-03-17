// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import io.modelcontextprotocol.spec.McpSchema.TextContent
import io.modelcontextprotocol.spec.parameters
import kotlinx.coroutines.reactor.awaitSingle
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.core.Failure
import org.eclipse.lmos.arc.core.Success
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.time.Instant
import java.time.Instant.now
import java.util.concurrent.atomic.AtomicReference

/**
 * An instance of [LLMFunctionLoader] that loads functions from the Model Context Protocol (MCP) server.
 */
class McpTools(private val url: String, private val cacheDuration: Duration?) :
    LLMFunctionLoader, Closeable {

    private val log = LoggerFactory.getLogger(javaClass)
    private val clientBuilder = McpClientBuilder(url)
    private val toolCache = AtomicReference<ToolCacheEntry>()
    private val defaultCacheDuration = Duration.ofMinutes(1)

    override suspend fun load(): List<LLMFunction> {
        val cached = toolCache.get()
        if (cached != null && cached.createdAt.isAfter(now().minus(cacheDuration ?: defaultCacheDuration))) {
            return cached.tools
        }

        val result = clientBuilder.execute { client ->
            client.listTools().awaitSingle().tools.map { tool ->
                log.debug("Loaded tool: ${tool.name} from $url")
                object : LLMFunction {
                    override val name: String = tool.name
                    override val parameters = parameters(tool)
                    override val description: String = tool.description
                    override val group: String = "MCP"
                    override val isSensitive: Boolean = false

                    override suspend fun execute(input: Map<String, Any?>) = result<String, LLMFunctionException> {
                        clientBuilder.execute { client ->
                            val result = try {
                                client.callTool(CallToolRequest(tool.name, input)).awaitSingle()
                            } catch (e: Exception) {
                                failWith { LLMFunctionException("Failed to call MCP tool: ${tool.name}!", e) }
                            }
                            if (result.isError) {
                                failWith { LLMFunctionException("Failed to call MCP tool: ${tool.name}! Error: ${result.content}") }
                            }
                            result.content.joinToString(separator = "\n") { if (it is TextContent) it.text else it.toString() }
                        }.getOrThrow()
                    }
                }
            }
        }
        when (result) {
            is Failure -> {
                log.error("Failed to load tools from $url: $result")
            }

            is Success -> {
                log.debug("Loaded tools from $url: $result")
            }
        }
        if (result is Success) toolCache.set(ToolCacheEntry(now(), result.value))
        return result.getOrThrow()
    }

    override fun close() {
        clientBuilder.close()
    }
}

/**
 * Used to implement a simple cache for listing tools.
 */
private class ToolCacheEntry(val createdAt: Instant, val tools: List<LLMFunction>)
