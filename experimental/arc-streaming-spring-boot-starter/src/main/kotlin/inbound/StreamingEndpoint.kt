// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.ws.inbound

import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.reactor.asFlux
import kotlinx.coroutines.reactor.mono
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agents.conversation.WritableDataStream
import org.eclipse.lmos.arc.agents.conversation.readAllBytes
import org.eclipse.lmos.arc.api.AgentResult
import org.eclipse.lmos.arc.api.RequestEnvelope
import org.eclipse.lmos.arc.ws.AgentCaller
import org.slf4j.LoggerFactory
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.reactive.socket.WebSocketHandler
import org.springframework.web.reactive.socket.WebSocketMessage
import org.springframework.web.reactive.socket.WebSocketMessage.Type.BINARY
import org.springframework.web.reactive.socket.WebSocketMessage.Type.TEXT
import org.springframework.web.reactive.socket.WebSocketSession
import org.springframework.web.server.ServerWebExchange
import java.util.concurrent.atomic.AtomicReference

/**
 * A WebSocket handler that streams audio data to an agent and returns the result.
 */
class StreamingEndpoint(private val agentCaller: AgentCaller) : WebSocketHandler, CorsConfigurationSource {

    private data class Session(val id: String, val envelope: RequestEnvelope, val data: WritableDataStream)

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    private val log = LoggerFactory.getLogger(javaClass)

    override fun handle(session: WebSocketSession) = mono<Void> {
        log.debug("Starting session: ${session.id}")
        try {
            val outChannel = Channel<WebSocketMessage>(capacity = Channel.UNLIMITED)
            val sessionHolder = AtomicReference<Session>(null)

            session.send(outChannel.consumeAsFlow().asFlux()).subscribe()
            session.receive().asFlow().collect { message ->
                when (message.type) {
                    TEXT -> async {
                        handleTextMessage(session, message, outChannel, sessionHolder)
                        session.close().subscribe()
                    }

                    BINARY -> handleBinaryMessage(session, message, sessionHolder)
                    else -> {}
                }
            }
        } catch (e: Exception) {
            log.error("Error handling session: ${session.id}", e)
        }
        log.debug("Closing session: ${session.id}")
        null
    }

    private suspend fun handleTextMessage(
        websocketSession: WebSocketSession,
        message: WebSocketMessage,
        output: Channel<WebSocketMessage>,
        sessionHolder: AtomicReference<Session>,
    ) {
        val msg = message.payloadAsText
        log.info("Session: ${websocketSession.id} received message: $msg")
        val envelope = json.decodeFromString(RequestEnvelope.serializer(), msg)
        val session = Session(websocketSession.id, envelope, WritableDataStream())
        sessionHolder.set(session)

        log.info("Sending session: ${websocketSession.id}")
        agentCaller.callAgent(
            session.envelope.agentName,
            session.envelope.payload,
            session.data,
        ).flatMapConcat { (result, dataStream) ->
            flow {
                val jsonResult = json.encodeToString(AgentResult.serializer(), result)
                emit(websocketSession.textMessage(jsonResult))
                dataStream?.let { stream ->
                    val bytes = stream.readAllBytes()
                    emit(websocketSession.binaryMessage { it.wrap(bytes) })
                }
            }
        }.collect {
            log.debug("Sending Agent Response: $it")
            output.send(it)
        }
    }

    private fun handleBinaryMessage(
        session: WebSocketSession,
        message: WebSocketMessage,
        sessionHolder: AtomicReference<Session>,
    ) {
        log.info("Received binary message for: ${session.id}")
        sessionHolder.get()?.let {
            val bytes = message.payload.asInputStream().use { b -> b.readAllBytes() }
            log.info("Received ${bytes.size} bytes")
            sessionHolder.get()?.data?.write(bytes)
        } ?: {
            log.warn("Received binary message without a session: ${session.id}!")
        }
    }

    override fun getCorsConfiguration(exchange: ServerWebExchange): CorsConfiguration {
        return CorsConfiguration().apply {
            allowCredentials = false
            allowedOrigins = listOf("*")
            allowedMethods = listOf("GET, PUT, POST, DELETE, OPTIONS")
            maxAge = 3600
        }
    }
}
