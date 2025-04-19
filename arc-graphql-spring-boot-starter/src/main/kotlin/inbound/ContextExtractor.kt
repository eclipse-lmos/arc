// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import org.eclipse.lmos.arc.agents.agent.AgentChain
import org.eclipse.lmos.arc.api.AgentRequest

/**
 * Extract context beans that are encoded in the AgentRequest.
 */
fun extractContext(request: AgentRequest): Set<Any> {
    return buildSet {
        /**
         * Extract an AgentChain from the request.
         */
        request.systemContext.firstOrNull { it.key == "agent_chain" }?.let { item ->
            add(AgentChain(item.value.split(",").map { it.trim() }.filter { it.isNotBlank() }))
        }
    }
}
