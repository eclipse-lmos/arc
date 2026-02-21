// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.adl.server.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.llm.LLMFinishedEvent
import io.ktor.sse.*
import kotlinx.serialization.json.Json
import org.eclipse.lmos.arc.agents.functions.LLMFunctionCalledEvent
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.LoggerFactory

class ClientEventPublisher : EventHandler<Event> {

    private val mutableEvents = MutableSharedFlow<ServerSentEvent>(replay = 0, extraBufferCapacity = 64)
    private val json = jacksonObjectMapper()
    private val log = LoggerFactory.getLogger(this::class.java)

    val events: SharedFlow<ServerSentEvent> = mutableEvents.asSharedFlow()

    suspend fun publish(event: ServerSentEvent) {
        mutableEvents.emit(event)
    }

    fun tryPublish(event: ServerSentEvent): Boolean {
        log.info("Trying to publish event: ${event.event} ${event.data}")
        return mutableEvents.tryEmit(event)
    }

    override fun onEvent(event: Event) {
        when (event) {
            is LLMFinishedEvent -> {
                tryPublish(
                    ServerSentEvent(
                        // event = "LLMFinishedEvent",
                        data = json.writeValueAsString(
                            mapOf(
                                "event" to "LLMFinishedEvent",
                                "model" to event.model,
                                "result" to (event.result.getOrNull()?.content ?: event.result.toString()),
                                "totalTokens" to event.totalTokens,
                                "promptTokens" to event.promptTokens,
                                "completionTokens" to event.completionTokens,
                                "toolCallCount" to event.functionCallCount,
                            )
                        )
                    )
                )
            }

            is LLMFunctionCalledEvent -> {
                if (false) tryPublish(
                    ServerSentEvent(
                        data = json.writeValueAsString(
                            mapOf(
                                "event" to "ToolCalledEvent",
                                "tool" to event.name,
                                "description" to event.description,
                                "parameters" to event.param,
                                "result" to (event.result.getOrNull() ?: event.result.toString())
                            )
                        )
                    )
                )
            }

            else -> {}
        }
    }
}
