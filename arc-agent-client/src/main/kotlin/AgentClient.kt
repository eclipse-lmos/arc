// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agent.client

import kotlinx.coroutines.flow.Flow
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.api.AgentResult

/**
 * Client for communicating with the Agents.
 */
interface AgentClient {

    /**
     * Calls the agent with the given request.
     * If an url is not provided, the client will use the default url.
     * If an agent name is not provided, the server will use the default agent.
     */
    suspend fun callAgent(agentRequest: AgentRequest, agentName: String? = null, url: String? = null, requestHeaders: Map<String, Any> = emptyMap()): Flow<AgentResult>
}

/**
 * Exception thrown when an error occurs during agent communication.
 */
class AgentException(message: String) : Exception(message)
