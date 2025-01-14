// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Subscription
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature.UseJavaDurationConversion
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.withLogContext
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * Provides an graphql subscription for events.
 */
class EventSubscription(private val allEventsHolder: EventSubscriptionHolder) : Subscription {

    @GraphQLDescription("Subscribes to events.")
    fun events(): Flow<AgentEvent> = allEventsHolder.flow()
}

class EventSubscriptionHolder : EventHandler<Event> {

    private val eventFlows = ConcurrentHashMap<String, Channel<AgentEvent>>()
    private val scope = CoroutineScope(SupervisorJob())
    private val objectMapper = jacksonMapperBuilder {
        enable(UseJavaDurationConversion)
    }.addModule(JavaTimeModule()).build()
    private val log = LoggerFactory.getLogger(javaClass)

    override fun onEvent(event: Event) {
        if (eventFlows.isEmpty()) return
        eventFlows.forEach { (id, channel) ->
            val conversationId = MDC.get("conversationId") ?: ""
            log.debug("Sending event: $id $event")
            val logContext = MDC.getCopyOfContextMap() ?: emptyMap()
            scope.launch {
                withLogContext(logContext) {
                    withTimeoutOrNull(20_000) {
                        channel.send(
                            AgentEvent(
                                event::class.simpleName.toString(),
                                objectMapper.writeValueAsString(event),
                                conversationId,
                                MDC.get("turnId"),
                            ),
                        )
                    } ?: run {
                        log.warn("Event flow $id is slow, closing it")
                        channel.close()
                    }
                }
            }
        }
    }

    fun flow(): Flow<AgentEvent> {
        val channel = Channel<AgentEvent>()
        val id = UUID.randomUUID().toString()
        eventFlows[id] = channel
        channel.invokeOnClose {
            log.info("Closing event flow: $id")
            eventFlows.remove(id)
        }
        log.info("Opening event flow: $id")
        return channel.receiveAsFlow()
    }
}

/**
 * Wraps an Agent event for transport.
 */
data class AgentEvent(
    val type: String,
    val payload: String,
    val conversationId: String?,
    val turnId: String?,
)
