// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.tracing

import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.withLogContext

/**
 * Tracer for agents.
 */
interface AgentTracer {

    suspend fun init(beanProvider: BeanProvider): AgentTracer = this

    suspend fun <T> withSpan(name: String, attributes: Map<String, String>, fn: suspend () -> T): T
}

/**
 * Default implementation of [AgentTracer] that sets log context.
 */
class DefaultAgentTracer : AgentTracer {

    override suspend fun <T> withSpan(name: String, attributes: Map<String, String>, fn: suspend () -> T): T {
        return withLogContext(attributes) {
            fn()
        }
    }
}
