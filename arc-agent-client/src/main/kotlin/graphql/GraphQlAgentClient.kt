// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client.graphql

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.websocket.*
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agent.client.AgentClient
import org.eclipse.lmos.arc.agent.client.AgentException
import org.eclipse.lmos.arc.api.AgentRequest
import org.slf4j.LoggerFactory
import java.io.Closeable
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Implementation of [AgentClient] that uses GraphQL over WebSockets to communicate with the agents.
 * See https://github.com/enisdenjo/graphql-ws/blob/master/PROTOCOL.md
 */
class GraphQlAgentClient(private val defaultUrl: String? = null) : AgentClient, Closeable {

    private val log = LoggerFactory.getLogger(this.javaClass)
    private val closing = AtomicBoolean(false)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val client = HttpClient(CIO) {
        install(WebSockets) {
            pingInterval = 20.seconds
        }
    }

    override suspend fun callAgent(
        agentRequest: AgentRequest,
        agentName: String?,
        url: String?,
        requestHeaders: Map<String, Any>,
    ) = flow {
        if (url == null && defaultUrl == null) error("Agent Url not provided!")
        val opId = UUID.randomUUID().toString()
        client.webSocket(url ?: defaultUrl!!, {
            requestHeaders.forEach { (key, value) -> header(key, value.toString()) }
        }) {
            initConnection()
            sendSubscription(opId, agentRequest, agentName)
            while (closing.get().not()) {
                when (val next = nextMessage()) {
                    is NextMessage -> {
                        if (next.id != opId) {
                            log.debug("Ignoring message with unexpected id: ${next.id}")
                            continue
                        }
                        emit(next.payload.data.agent)
                    }

                    is CompleteMessage -> break
                    is ErrorMessage -> throw AgentException("Error received for $opId! Error:[$next]")
                    else -> {}
                }
            }
            close()
        }
    }

    private suspend fun DefaultClientWebSocketSession.initConnection() {
        sendMessage(InitConnectionMessage)
        val response = nextMessage()
        if (response !is AckConnectionMessage) {
            throw AgentException("Connection not acknowledged! Received $response")
        }
    }

    private suspend fun DefaultClientWebSocketSession.sendSubscription(
        opId: String,
        agentRequest: AgentRequest,
        agentName: String?,
    ) {
        sendMessage(
            SubscribeMessage(
                opId,
                ClientPayload(AGENT_SUBSCRIPTION, AgentRequestVariables(agentRequest, agentName)),
            ),
        )
    }

    private suspend fun DefaultClientWebSocketSession.sendMessage(message: ClientMessage) {
        val jsonCall = json.encodeToString(message)
        log.trace("Sending $jsonCall")
        send(Frame.Text(jsonCall))
    }

    private suspend fun DefaultClientWebSocketSession.nextMessage(): ServerMessage {
        val response = incoming.receive() as Frame.Text
        return response.readText().let {
            log.trace("Received $it")
            json.decodeFromString(it)
        }
    }

    override fun close() {
        closing.set(true)
        client.close()
    }
}
