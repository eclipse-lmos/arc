// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeout
import org.eclipse.lmos.arc.agents.dsl.AgentDefinition
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.events.BaseEvent
import org.eclipse.lmos.arc.agents.events.Event
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTime

private val rateLimiters = ConcurrentHashMap<String, Semaphore>()
private val releaseScope = CoroutineScope(SupervisorJob() + IO)

/**
 * A simple Rate Limiter that can be used as a top level Agent function.
 * See tests for examples, RateLimiterTest.kt.
 */
fun AgentDefinition.limit(newName: String? = null, fn: RateLimiterContext.() -> Rate) {
    filterInput {
        val limiterName = newName ?: "agent/$name"
        limit(limiterName, fn) ?: throw RateLimitTimeoutException(limiterName)
    }
}

suspend fun DSLContext.limit(name: String, fn: RateLimiterContext.() -> Rate): String? {
    val context = RateLimiterContext(this)
    val rate = context.fn()
    val timeout = rate.timeout ?: 30.seconds
    val rateLimiterName = "$name/${rate.limit}"

    val rateLimiter = rateLimiters.computeIfAbsent(rateLimiterName) { Semaphore(rate.limit) }
    return tracer().withSpan("limit $name") { tags ->
        tags.tag("rate.limit.rate", rate.toString())
        if (rateLimiter.tryAcquire()) return@withSpan name.also { releasePermitLater(rateLimiterName, rate) }
        try {
            withTimeout(timeout) {
                val duration = measureTime { rateLimiter.acquire() }
                releasePermitLater(rateLimiterName, rate)
                emit(RateLimitedEvent(name, duration))
            }
            tags.tag("rate.limit.timeout", "false")
            name
        } catch (e: TimeoutCancellationException) {
            emit(RateLimitTimeoutEvent(name, timeout, rate.fallback != null))
            rate.fallback?.invoke()
            tags.tag("rate.limit.timeout", "true")
            null
        }
    }
}

private fun releasePermitLater(name: String, rate: Rate) {
    releaseScope.launch {
        delay(rate.duration)
        rateLimiters[name]?.release()
    }
}

/**
 * Rate limit Event
 */
sealed class RateLimitEvent(val name: String) : Event by BaseEvent()
class RateLimitedEvent(name: String, val duration: kotlin.time.Duration) : RateLimitEvent(name)
class RateLimitTimeoutEvent(name: String, val duration: kotlin.time.Duration, val fallback: Boolean) :
    RateLimitEvent(name)

/**
 * Rate limit Exception
 */
class RateLimitTimeoutException(name: String) : Exception("Rate limit exceeded for $name!")

/**
 * Context for defining rate limits.
 */
class RateLimiterContext(context: DSLContext) : DSLContext by context {

    operator fun Int.div(other: kotlin.time.Duration) = Rate(this, other)

    infix fun Rate.withTimeout(other: kotlin.time.Duration) = copy(timeout = other)

    infix fun Rate.fallback(other: suspend () -> Unit) = copy(fallback = other)
}

data class Rate(
    val limit: Int,
    val duration: kotlin.time.Duration,
    val timeout: kotlin.time.Duration? = null,
    val fallback: (suspend () -> Unit)? = null,
)
