// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.ws

import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.graphql.AgentResolver
import org.eclipse.lmos.arc.graphql.ContextHandler
import org.eclipse.lmos.arc.graphql.EmptyContextHandler
import org.eclipse.lmos.arc.graphql.ErrorHandler
import org.eclipse.lmos.arc.ws.inbound.StreamingEndpoint
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.reactive.HandlerMapping
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping
import org.springframework.web.reactive.socket.server.WebSocketService
import org.springframework.web.reactive.socket.server.support.HandshakeWebSocketService
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter

@AutoConfiguration
open class AgentStreamingAutoConfiguration {

    @Bean
    fun handlerAdapter() =
        WebSocketHandlerAdapter(webSocketService())

    @Bean
    fun webSocketService(): WebSocketService {
        val strategy = org.springframework.web.reactive.socket.server.upgrade.TomcatRequestUpgradeStrategy().apply {
            setMaxBinaryMessageBufferSize(1024 * 1024 * 35) // 15MB
        }
        return HandshakeWebSocketService(strategy)
    }

    @Bean
    fun handlerMapping(streamingEndpoint: StreamingEndpoint): HandlerMapping {
        val map = mapOf("/stream/agent" to streamingEndpoint)
        val order = -1
        return SimpleUrlHandlerMapping(map, order)
    }

    @Bean
    fun agentCaller(
        agentProvider: AgentProvider,
        errorHandler: ErrorHandler? = null,
        contextHandler: ContextHandler? = null,
        agentResolver: AgentResolver? = null,
    ) = AgentCaller(agentProvider, errorHandler, contextHandler ?: EmptyContextHandler(), agentResolver)

    @Bean
    fun streamingEndpoint(agentCaller: AgentCaller) = StreamingEndpoint(agentCaller)
}
