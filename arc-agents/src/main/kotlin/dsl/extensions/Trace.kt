// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl.extensions

import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.DefaultAgentTracer

/**
 * Returns the AgentTracer from the context or a default implementation.
 */
suspend fun DSLContext.tracer(): AgentTracer {
    return getOptional<AgentTracer>() ?: DefaultAgentTracer()
}
