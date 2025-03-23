// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner.server

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSubscriptionsRoute
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.eclipse.lmos.arc.graphql.inbound.AgentQuery
import org.eclipse.lmos.arc.graphql.inbound.AgentSubscription
import org.eclipse.lmos.arc.graphql.inbound.EventSubscription
import kotlin.time.Duration.Companion.seconds

/**
 * Starts the ARC server.
 */
fun runApp(appConfig: AppConfig) {
    val (agentProvider, eventSubscriptionHolder) = setupArc(appConfig)

    embeddedServer(Netty, port = 8080) {
        install(GraphQL) {
            schema {
                packages = listOf("org.eclipse.lmos.arc.api", "org.eclipse.lmos.arc.graphql.inbound")
                queries = listOf(
                    AgentQuery(agentProvider),
                )
                subscriptions = listOf(
                    AgentSubscription(agentProvider),
                    EventSubscription(eventSubscriptionHolder),
                )
            }
        }

        install(StatusPages) {
            defaultGraphQLStatusPages()
        }

        install(WebSockets) {
            pingPeriod = 1.seconds
            contentConverter = JacksonWebsocketContentConverter()
        }

        install(RoutingRoot) {
            staticResources("/chat", "/chat")
            graphQLPostRoute()
            graphQLSubscriptionsRoute()
        }

        install(CORS) {
            anyHost()
            allowHeader(HttpHeaders.ContentType)
            allowHeader(HttpHeaders.Authorization)
            allowMethod(HttpMethod.Get)
            allowMethod(HttpMethod.Post)
            allowMethod(HttpMethod.Put)
        }
    }.start(wait = true)
}
