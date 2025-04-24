// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.server.ktor

import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.routing.*
import org.eclipse.lmos.arc.agents.DSLAgents
import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSubscriptionsRoute
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.websocket.*
import org.eclipse.lmos.arc.graphql.inbound.AgentQuery
import org.eclipse.lmos.arc.graphql.inbound.AgentSubscription
import org.eclipse.lmos.arc.graphql.inbound.EventSubscription
import org.eclipse.lmos.arc.graphql.inbound.EventSubscriptionHolder
import kotlin.time.Duration.Companion.seconds
import io.ktor.server.http.content.*
import org.eclipse.lmos.arc.agents.ArcAgents

/**
 * Starts a Ktor server that exposes the agents and their functionality through a GraphQL API.
 *
 * The server provides:
 * - GraphQL POST endpoint for queries and mutations
 * - GraphQL WebSocket endpoint for subscriptions
 * - Static resources for a chat UI
 *
 * @param wait If true, the current thread will be blocked until the server is stopped.
 *            If false, the server will start in a separate thread and the function will return immediately.
 * @param port The port on which the server will listen for incoming connections.
 * @param module Additional Ktor module configuration to be applied to the server.
 */
fun ArcAgents.serve(wait: Boolean = true, port: Int = 8080, module: Application.() -> Unit = {}) {
    val eventSubscriptionHolder = EventSubscriptionHolder()
    add(eventSubscriptionHolder)

    embeddedServer(CIO, port = port) {
        install(GraphQL) {
            schema {
                packages = listOf("org.eclipse.lmos.arc.api", "org.eclipse.lmos.arc.graphql.inbound")
                queries = listOf(
                    AgentQuery(this@serve),
                )
                subscriptions = listOf(
                    AgentSubscription(this@serve),
                    EventSubscription(eventSubscriptionHolder),
                )
            }
        }

        install(StatusPages) {
            defaultGraphQLStatusPages()
        }

        install(WebSockets) {
            pingPeriod = 10.seconds
            contentConverter = JacksonWebsocketContentConverter()
        }

        install(RoutingRoot) {
            staticResources("/chat", "/chat")
            graphQLPostRoute()
            graphQLSubscriptionsRoute()
        }

        module()

    }.start(wait = wait)
}
