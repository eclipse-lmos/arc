// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.server.ktor

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.defaultGraphQLStatusPages
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import com.expediagroup.graphql.server.ktor.graphQLSubscriptionsRoute
import io.ktor.http.ContentType.Application.Json
import io.ktor.http.HttpStatusCode.Companion.OK
import io.ktor.http.HttpStatusCode.Companion.ServiceUnavailable
import io.ktor.serialization.jackson.*
import io.ktor.server.application.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.http.content.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agents.ArcAgents
import org.eclipse.lmos.arc.agents.agent.health
import org.eclipse.lmos.arc.graphql.inbound.AgentQuery
import org.eclipse.lmos.arc.graphql.inbound.AgentSubscription
import org.eclipse.lmos.arc.graphql.inbound.EventSubscription
import org.eclipse.lmos.arc.graphql.inbound.EventSubscriptionHolder
import kotlin.time.Duration.Companion.seconds

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
 * @param extraRoots Additional routing configuration to be applied to the server.
 * @param module Additional Ktor module configuration to be applied to the server.
 */
fun ArcAgents.serve(
    wait: Boolean = true,
    port: Int? = null,
    module: Application.() -> Unit = {},
    extraRoots: Route.() -> Unit = {},
    devMode: Boolean? = null,
) {
    val eventSubscriptionHolder = EventSubscriptionHolder()
    add(eventSubscriptionHolder)

    val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        isLenient = true
    }

    embeddedServer(CIO, port = port ?: EnvConfig.serverPort) {
        install(GraphQL) {
            schema {
                packages = listOf("org.eclipse.lmos.arc.api", "org.eclipse.lmos.arc.graphql.inbound")
                queries = listOf(
                    AgentQuery(this@serve),
                )
                subscriptions = buildList {
                    add(AgentSubscription(this@serve))
                    if (devMode ?: EnvConfig.isDevMode) add(EventSubscription(eventSubscriptionHolder))
                }
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
            if (devMode ?: EnvConfig.isDevMode) staticResources("/chat", "/chat")
            graphQLPostRoute()
            graphQLSubscriptionsRoute()

            // Health endpoint
            get("/health") {
                val health = health()
                call.respondText(json.encodeToString(health), Json, if (health.ok) OK else ServiceUnavailable)
            }
            // Same implementation for readyness and liveness probes
            get("/health/*") {
                val health = health()
                call.respondText(json.encodeToString(health), Json, if (health.ok) OK else ServiceUnavailable)
            }

            extraRoots()
        }

        module()
    }.start(wait = wait)
}
