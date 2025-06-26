// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.api.AgentRequest

/**
 * Resolves the agent for the given request.
 * Returns null if no agent is found.
 */
interface AgentResolver {

    fun resolveAgent(agentName: String? = null, request: AgentRequest): Agent<*, *>?
}
