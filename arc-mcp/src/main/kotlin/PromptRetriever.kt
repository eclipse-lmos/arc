// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities
import io.modelcontextprotocol.spec.McpSchema.GetPromptRequest
import io.modelcontextprotocol.spec.McpSchema.TextContent
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import org.eclipse.lmos.arc.agents.dsl.extensions.NoPromptFoundException
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptException
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptRetriever
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptServerException
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.closeWith
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Executors

/**
 * PromptRetriever that uses the MCP client to fetch prompts.
 */
class MCPPromptRetriever(val url: String) : PromptRetriever {

    private val log = LoggerFactory.getLogger(javaClass)
    private val dispatcher = Executors.newVirtualThreadPerTaskExecutor().asCoroutineDispatcher()

    override suspend fun fetchPromptText(name: String, args: Map<String, Any?>): Result<String, PromptException> =
        result<String, PromptException> {
            withContext(dispatcher) {
                val result = try {
                    val transport = HttpClientSseClientTransport(url)
                    val client = McpClient.sync(transport)
                        .requestTimeout(Duration.ofSeconds(10))
                        .capabilities(
                            ClientCapabilities.builder().build(),
                        )
                        .build()
                    client.initialize() closeWith { client.closeGracefully() }

                    val prompt = client.getPrompt(GetPromptRequest(name, args))
                    (prompt.messages.lastOrNull()?.content as? TextContent)?.text
                } catch (e: Exception) {
                    failWith { PromptServerException("Failed to fetch prompt: $name", e) }
                }
                result ?: failWith { NoPromptFoundException(name) }
            }
        }
}
