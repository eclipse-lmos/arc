// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.mcp

/**
 * Wire transport used to reach a remote MCP server.
 *
 * Configure via `arc.mcp.tools.transport` (also used for [McpPromptRetriever] when enabled),
 * or `ARC_MCP_TOOLS_TRANSPORT` with [McpToolsByEnvironment].
 * `/sse` vs `POST …/mcp` is derived from [McpClientTransport] in [McpClientBuilder]; the configured URL can be the server base or include `/sse`.
 */
enum class McpClientTransport {
    /** MCP over HTTP Server-Sent Events (typical URL suffix `/sse`). */
    SSE,

    /** MCP Streamable HTTP (e.g. Spring AI stateless server `POST /mcp`). */
    STREAMABLE_HTTP,
    ;

    companion object {
        fun fromConfiguration(value: String): McpClientTransport {
            val n = value.trim().lowercase().replace('-', '_')
            return when (n) {
                "sse" -> SSE
                "streamable_http", "streamablehttp", "mcp" -> STREAMABLE_HTTP
                else -> throw IllegalArgumentException(
                    "Unknown MCP client transport '$value'. Expected one of: sse, mcp, streamable-http.",
                )
            }
        }
    }
}
