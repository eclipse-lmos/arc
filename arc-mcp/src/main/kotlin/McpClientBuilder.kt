// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.client.McpAsyncClient
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.closeWith
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration

/**
 * The MCP client.
 *
 * Todo See if we can keep the client open and reuse it. Currently, the client seems to lose connection after a while.
 */
class McpClientBuilder(private val url: String) : Closeable {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun <T> execute(fn: suspend (McpAsyncClient, String) -> T): Result<T, Exception> = result<T, Exception> {
        val client = createClient() closeWith { it.closeGracefully().subscribe() }
        val result = client.initialize().awaitSingleOrNull()
        log.debug("Client connected: $url Result: $result")
        fn(client, url)
    }

    private fun createClient(): McpAsyncClient {
        val fixed = if (url.endsWith("/")) url.substring(0, url.length - 1) else url
        return McpClient.async(HttpClientSseClientTransport.builder(fixed).build())
            .requestTimeout(Duration.ofSeconds(10))
            .capabilities(ClientCapabilities.builder().build())
            .build()
    }

    override fun close() {
        // client.close()
    }
}
