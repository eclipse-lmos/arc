// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.tracing

import org.eclipse.lmos.arc.agents.withLogContext

/**
 * Tracer for agents.
 */
interface AgentTracer {

    suspend fun <T> withSpan(
        name: String,
        attributes: Map<String, String> = emptyMap(),
        fn: suspend (Tags, Events) -> T,
    ): T
}

/**
 * Tag interface for setting tags on spans.
 */
interface Tags {
    fun tag(key: String, value: String)

    fun tag(key: String, value: Long)
}

object NoopTags : Tags {
    override fun tag(key: String, value: String) {
        // no-op
    }

    override fun tag(key: String, value: Long) {
        // no-op
    }
}

fun interface Events {
    fun event(key: String, value: String)
}

/**
 * Default implementation of [AgentTracer] that sets log context.
 */
class DefaultAgentTracer : AgentTracer {

    override suspend fun <T> withSpan(
        name: String,
        attributes: Map<String, String>,
        fn: suspend (Tags, Events) -> T,
    ): T {
        return withLogContext(attributes) {
            fn(NoopTags, { _, _ -> })
        }
    }
}
