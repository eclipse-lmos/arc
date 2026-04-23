// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.mcp

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class McpClientTransportTest {

    @Test
    fun `parses sse variants`() {
        assertThat(McpClientTransport.fromConfiguration("sse")).isEqualTo(McpClientTransport.SSE)
        assertThat(McpClientTransport.fromConfiguration(" SSE ")).isEqualTo(McpClientTransport.SSE)
    }

    @Test
    fun `parses streamable variants`() {
        assertThat(McpClientTransport.fromConfiguration("mcp")).isEqualTo(McpClientTransport.STREAMABLE_HTTP)
        assertThat(McpClientTransport.fromConfiguration("streamable-http")).isEqualTo(McpClientTransport.STREAMABLE_HTTP)
        assertThat(McpClientTransport.fromConfiguration("streamable_http")).isEqualTo(McpClientTransport.STREAMABLE_HTTP)
    }

    @Test
    fun `rejects unknown`() {
        assertThatThrownBy { McpClientTransport.fromConfiguration("websocket") }
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
