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
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean

/**
 * The MCP client.
 */
class McpClientBuilder(private val url: String) : Closeable {

    private val log = LoggerFactory.getLogger(javaClass)
    private val client = McpClient.async(HttpClientSseClientTransport(url))
        .requestTimeout(Duration.ofSeconds(10))
        .capabilities(ClientCapabilities.builder().build())
        .build()
    private val initialized = AtomicBoolean(false)

    suspend fun <T> execute(fn: suspend (McpAsyncClient) -> T): Result<T, Exception> = result<T, Exception> {
        if (!initialized.getAndSet(true)) {
            val result = client.initialize().awaitSingleOrNull()
            log.debug("Client connected: $url Result: $result")
        }
        fn(client)
    }

    override fun close() {
        client.close()
    }
}
