// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.isPresent
import kotlinx.coroutines.withContext
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.llm.LLMFinishedEvent
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.agents.withLogContext
import org.eclipse.lmos.arc.core.getOrNull
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.context.annotation.Bean

@ConditionalOnClass(Tracer::class)
class TracingConfiguration {

    @Bean
    fun tracer(tracer: Tracer) = SpanTracer(tracer)
}

/**
 * Agent Tracer backed by a Micrometer Tracer.
 */
class SpanTracer(private val tracer: Tracer) :
    AgentTracer,
    EventHandler<Event> {

    /**
     * We keep track of the current span ourselves to fix issues with the Micrometer Tracer and Kotlin Coroutines.
     */
    private val contextLocal = ThreadLocal<Span?>()

    override suspend fun <T> withSpan(name: String, attributes: Map<String, String>, fn: suspend (Tags) -> T): T {
        return withLogContext(attributes) {
            val parent = if (contextLocal.isPresent()) contextLocal.get() else null

            if (parent == null) {
                val newScope = tracer.traceContextBuilder()
                    .apply { MDC.get("conversationId")?.let { traceId(it) } }
                    .sampled(false)
                    .build()
                val scope = tracer.currentTraceContext().newScope(newScope)
                scope.use {
                    val newSpan = tracer.nextSpan().name(name).start()
                    try {
                        return@withLogContext withContext(contextLocal.asContextElement(value = newSpan)) {
                            tracer.withSpan(newSpan).use {
                                attributes.forEach { (k, v) -> newSpan.tag(k, v) }
                                fn({ k, v -> newSpan.tag(k, v) })
                            }.also {
                                contextLocal.remove()
                            }
                        }
                    } finally {
                        newSpan.end()
                    }
                }
            }

            val newSpan = tracer.nextSpan(parent)?.name(name) ?: return@withLogContext fn({ _, _ -> })
            try {
                val startedSpan = newSpan.start()
                return@withLogContext withContext(contextLocal.asContextElement(value = startedSpan)) {
                    tracer.withSpan(startedSpan).use {
                        attributes.forEach { (k, v) -> newSpan.tag(k, v) }
                        fn({ k, v -> newSpan.tag(k, v) })
                    }.also {
                        contextLocal.remove()
                    }
                }
            } finally {
                newSpan.end()
            }
        }
    }

    override fun onEvent(event: Event) {
        when (event) {
            is LLMFinishedEvent -> handleLLMFinishedEvent(event)
        }
    }

    private fun handleLLMFinishedEvent(event: LLMFinishedEvent) {
        val name = "chat ${event.model}"
        val parent = contextLocal.get()
        val newSpan =
            if (parent != null) tracer.nextSpan(parent)!!.name(name) else tracer.nextSpan().name(name)
        try {
            tracer.withSpan(newSpan.start()).use {
                newSpan.tag("gen_ai.request.model", event.model)
                newSpan.tag("gen_ai.operation.name", "chat")
                newSpan.tag("gen_ai.response.finish_reasons", event.finishReasons?.toString() ?: "[]")
                event.settings?.seed?.let { newSpan.tag("gen_ai.request.seed", it) }
                event.settings?.temperature?.let { newSpan.tag("gen_ai.request.temperature", it) }
                event.settings?.topP?.let { newSpan.tag("gen_ai.request.top_p", it) }
                newSpan.tag("gen_ai.user.message", event.messages.last().content)
                newSpan.tag("gen_ai.assistant.message", event.result.getOrNull()?.content ?: "")
            }
        } finally {
            newSpan.end()
        }
    }
}
