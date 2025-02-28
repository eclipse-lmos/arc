package org.eclipse.lmos.arc.agents

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanBuilder
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.extension.kotlin.asContextElement
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


val tracer = GlobalOpenTelemetry.getTracer("lmos-arc-agents")

suspend fun <T> Tracer.startSpan(
    spanName: String,
    parameters: (SpanBuilder.() -> Unit)? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (span: Span) -> T
): T {
    val span: Span = this.spanBuilder(spanName).run {
        if (parameters != null) parameters()
        startSpan()
    }

    return withContext(coroutineContext + span.asContextElement()) {
        try {
            block(span)
        } catch (throwable: Throwable) {
            span.setStatus(StatusCode.ERROR)
            span.recordException(throwable)
            throw throwable
        } finally {
            span.end()
        }
    }
}

suspend fun <T> withSpan(
    spanName: String,
    parameters: (SpanBuilder.() -> Unit)? = null,
    coroutineContext: CoroutineContext = EmptyCoroutineContext,
    block: suspend (span: Span) -> T
): T = tracer.startSpan(spanName, parameters, coroutineContext, block)