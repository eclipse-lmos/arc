// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.spec.McpSchema
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import io.modelcontextprotocol.spec.McpSchema.TextContent
import kotlinx.coroutines.reactor.awaitSingle
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
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

    override suspend fun load(context: ToolLoaderContext?): List<LLMFunction> {
        val cached = toolCache.get()
        if (cached != null && cached.createdAt.isAfter(now().minus(cacheDuration ?: defaultCacheDuration))) {
            return cached.tools
        }

        val result = clientBuilder.execute { client, _ ->
            client.listTools().awaitSingle().tools.map { tool ->
                log.debug("Loaded tool: ${tool.name} from $url")
                ToolWrapper(tool, clientBuilder)
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

/**
 * Maps to the MCP tool "_meta" field.
 */
data class ToolCallMetadata(val data: Map<String, Any>)

class ToolWrapper(
    private val tool: McpSchema.Tool,
    private val clientBuilder: McpClientBuilder,
    private val context: DSLContext? = null,
) :
    LLMFunction,
    FunctionWithContext {
    override val name: String = tool.name
    override val version: String? = null
    override val outputDescription: String? = null
    override val parameters = parameters(tool)
    override val description: String = tool.description
    override val group: String = "MCP"
    override val isSensitive: Boolean = false

    override suspend fun execute(input: Map<String, Any?>) = result<String, LLMFunctionException> {
        clientBuilder.execute { client, url ->
            val result = try {
                val meta = context?.getOptional<McpToolMetadataProvider>()?.provide(tool.name, url)
                client.callTool(CallToolRequest(tool.name, input, meta?.data ?: emptyMap())).awaitSingle()
            } catch (e: Exception) {
                failWith { LLMFunctionException("Failed to call MCP tool: ${tool.name}!", e) }
            }
            if (result.isError) {
                failWith { LLMFunctionException("Failed to call MCP tool: ${tool.name}! Error: ${result.content}") }
            }
            result.content.joinToString(separator = "\n") { if (it is TextContent) it.text else it.toString() }
        }.getOrThrow()
    }

    override fun withContext(context: DSLContext): LLMFunction {
        return ToolWrapper(tool, clientBuilder, context)
    }
}

/**
 * Provider for MCP tool metadata.
 */
interface McpToolMetadataProvider {

    /**
     * Provide metadata for a tool.
     *
     * @param toolName The name of the tool.
     * @param mcpServerUrl The URL of the MCP server.
     * @return The metadata for the tool.
     */
    fun provide(toolName: String, mcpServerUrl: String): ToolCallMetadata?
}
