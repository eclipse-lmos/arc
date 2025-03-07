// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities
import io.modelcontextprotocol.spec.McpSchema.TextContent
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionException
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.ParametersSchema
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.time.Duration

/**
 * An instance of [LLMFunctionLoader] that loads functions from the Model Context Protocol (MCP) server.
 */
class McpTools(private val url: String) : LLMFunctionLoader {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun load(): List<LLMFunction> {
        val result = result<List<LLMFunction>, Exception> {
            val transport = HttpClientSseClientTransport(url)
            val client = McpClient.sync(transport)
                .requestTimeout(Duration.ofSeconds(10))
                .capabilities(
                    ClientCapabilities.builder().build(),
                )
                .build()
            client.initialize()

            // TODO tool.inputSchema.convert()
            client.listTools().tools.map { tool ->
                object : LLMFunction {
                    override val name: String = tool.name
                    override val parameters = ParametersSchema()
                    override val description: String = tool.description
                    override val group: String = "MCP"
                    override val isSensitive: Boolean = false

                    override suspend fun execute(input: Map<String, Any?>) = result<String, LLMFunctionException> {
                        val result = try {
                            client.callTool(CallToolRequest(tool.name, input))
                        } catch (e: Exception) {
                            failWith { LLMFunctionException("Failed to call MCP tool: ${tool.name}!", e) }
                        }
                        if (result.isError) {
                            failWith { LLMFunctionException("Failed to call MCP tool: ${tool.name}! Error: ${result.content}") }
                        }
                        result.content.joinToString(separator = "\n") { if (it is TextContent) it.text else it.toString() }
                    }
                }
            }
        }
        return result.getOrThrow()
    }
}
