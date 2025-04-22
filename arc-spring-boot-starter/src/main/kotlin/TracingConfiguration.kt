// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import io.micrometer.tracing.Span
import io.micrometer.tracing.Tracer
import kotlinx.coroutines.asContextElement
import kotlinx.coroutines.isPresent
import kotlinx.coroutines.withContext
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.Events
import org.eclipse.lmos.arc.agents.tracing.NoopTags
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.eclipse.lmos.arc.agents.withLogContext
import org.slf4j.MDC
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean

@ConditionalOnClass(Tracer::class)
class TracingConfiguration {

    @Bean
    @ConditionalOnMissingBean
    fun tracer(tracer: Tracer): AgentTracer = SpanTracer(tracer)
}

/**
 * Agent Tracer backed by a Micrometer Tracer.
 */
class SpanTracer(private val tracer: Tracer) : AgentTracer {

    /**
     * We keep track of the current span ourselves to fix issues with the Micrometer Tracer and Kotlin Coroutines.
     */
    private val contextLocal = ThreadLocal<Span?>()

    override suspend fun <T> withSpan(
        name: String,
        attributes: Map<String, String>,
        fn: suspend (Tags, Events) -> T,
    ): T {
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
                                fn(
                                    object : Tags {
                                        override fun tag(key: String, value: String) {
                                            newSpan.tag(key, value)
                                        }

                                        override fun tag(key: String, value: Long) {
                                            newSpan.tag(key, value)
                                        }

                                        override fun tag(key: String, value: Boolean) {
                                            newSpan.tag(key, value)
                                        }

                                        override fun error(ex: Throwable) {
                                            newSpan.error(ex)
                                        }
                                    },
                                    { k, v -> },
                                )
                            }.also {
                                contextLocal.remove()
                            }
                        }
                    } finally {
                        newSpan.end()
                    }
                }
            }

            val newSpan = tracer.nextSpan(parent)?.name(name) ?: return@withLogContext fn(NoopTags, { _, _ -> })
            try {
                val startedSpan = newSpan.start()
                return@withLogContext withContext(contextLocal.asContextElement(value = startedSpan)) {
                    tracer.withSpan(startedSpan).use {
                        attributes.forEach { (k, v) -> newSpan.tag(k, v) }
                        fn(
                            object : Tags {
                                override fun tag(key: String, value: String) {
                                    newSpan.tag(key, value)
                                }

                                override fun tag(key: String, value: Long) {
                                    newSpan.tag(key, value)
                                }

                                override fun tag(key: String, value: Boolean) {
                                    newSpan.tag(key, value)
                                }

                                override fun error(ex: Throwable) {
                                    newSpan.error(ex)
                                }
                            },
                            { k, v -> },
                        )
                    }.also {
                        contextLocal.remove()
                    }
                }
            } finally {
                newSpan.end()
            }
        }
    }
}
