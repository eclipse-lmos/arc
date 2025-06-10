// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0
package org.eclipse.lmos.arc.server.ktor

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode.ERROR
import io.opentelemetry.api.trace.StatusCode.OK
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.extension.kotlin.getOpenTelemetryContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.Events
import org.eclipse.lmos.arc.agents.tracing.Tags
import org.slf4j.LoggerFactory

/**
 * OpenTelemetry implementation of [AgentTracer].
 */
class OtelTracer : AgentTracer {

    private val log = LoggerFactory.getLogger(javaClass)
    private val tracer: Tracer

    init {
        System.setProperty("otel.java.global-autoconfigure.enabled", "true")
        System.setProperty("otel.metrics.exporter", "none")
        tracer = GlobalOpenTelemetry.getTracer("org.eclipse.lmos.arc")
    }

    override suspend fun <T> withSpan(
        name: String,
        attributes: Map<String, String>,
        fn: suspend (Tags, Events) -> T,
    ): T {
        log.debug("Starting span: $name with attributes: $attributes")
        val span = tracer.spanBuilder(name).startSpan()
        return withContext(span.asContextElement()) {
            try {
                val context = currentCoroutineContext().getOpenTelemetryContext()
                context.with(span).makeCurrent().use { _ ->
                    attributes.forEach { (k, v) -> span.setAttribute(k, v) }
                    val otelTags = OtelTags(span)
                    fn(otelTags, otelTags).also { span.setStatus(OK) }
                }
            } catch (ex: Throwable) {
                span.setStatus(ERROR)
                span.recordException(ex)
                throw ex
            } finally {
                span.end()
            }
        }
    }
}

class OtelTags(private val span: Span) : Tags, Events {

    override fun tag(key: String, value: String) {
        span.setAttribute(key, value)
    }

    override fun tag(key: String, value: Long) {
        span.setAttribute(key, value)
    }

    override fun tag(key: String, value: Boolean) {
        span.setAttribute(key, value)
    }

    override fun error(ex: Throwable) {
        span.recordException(ex)
    }

    override fun event(key: String, value: String) {
        span.addEvent(key, Attributes.builder().put(key, value).build())
    }
}
