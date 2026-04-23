// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

import io.modelcontextprotocol.client.McpAsyncClient
import io.modelcontextprotocol.client.McpClient
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport
import io.modelcontextprotocol.spec.McpSchema.ClientCapabilities
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.closeWith
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.net.URI
import java.time.Duration

/**
 * The MCP client.
 *
 * [transport] selects the wire protocol. Connection URLs are derived from [url]:
 * - **SSE:** `…/sse` (if [url] already ends with `/sse` it is kept; otherwise `/sse` is appended to the server origin).
 * - **Streamable HTTP:** server origin + `POST /mcp` (any `/sse` suffix in [url] is stripped for the HTTP client base).
 *
 * Todo See if we can keep the client open and reuse it. Currently, the client seems to lose connection after a while.
 */
class McpClientBuilder(
    private val url: String,
    private val transport: McpClientTransport = McpClientTransport.SSE,
) : Closeable {

    private val log = LoggerFactory.getLogger(javaClass)

    suspend fun <T> execute(fn: suspend (McpAsyncClient, String) -> T): Result<T, Exception> = result<T, Exception> {
        val client = createClient() closeWith { it.closeGracefully().subscribe() }
        val result = client.initialize().awaitSingleOrNull()
        log.debug("Client connected: $url transport=$transport Result: $result")
        fn(client, url)
    }

    private fun createClient(): McpAsyncClient {
        val mcpTransport = when (transport) {
            McpClientTransport.SSE ->
                HttpClientSseClientTransport.builder(sseConnectionUrl(url)).build()
            McpClientTransport.STREAMABLE_HTTP ->
                HttpClientStreamableHttpTransport.builder(streamableHttpBaseUrl(url)).endpoint("/mcp").build()
        }
        return McpClient.async(mcpTransport)
            .requestTimeout(Duration.ofSeconds(10))
            .capabilities(ClientCapabilities.builder().build())
            .build()
    }

    override fun close() {
        // client.close()
    }

    private companion object {
        private fun trimTrailingSlash(s: String): String = s.trimEnd('/')

        /** Base URL for streamable client: strip a trailing `/sse` path if present, else use as configured. */
        private fun streamableHttpBaseUrl(configUrl: String): String {
            val trimmed = trimTrailingSlash(configUrl)
            val path = URI.create(trimmed).path.orEmpty()
            if (!path.endsWith("/sse")) return trimmed
            val withoutSse = trimTrailingSlash(trimmed.removeSuffix("/sse"))
            return withoutSse.ifEmpty { originOnly(URI.create(trimmed)) }
        }

        private fun originOnly(uri: URI): String {
            val host = uri.host ?: throw IllegalArgumentException("Invalid MCP URL (missing host): $uri")
            val portPart = if (uri.port == -1) "" else ":${uri.port}"
            return "${uri.scheme}://$host$portPart"
        }

        private fun sseConnectionUrl(configUrl: String): String {
            val trimmed = trimTrailingSlash(configUrl)
            val path = URI.create(trimmed).path.orEmpty()
            return if (path.endsWith("/sse")) trimmed else "$trimmed/sse"
        }
    }
}
