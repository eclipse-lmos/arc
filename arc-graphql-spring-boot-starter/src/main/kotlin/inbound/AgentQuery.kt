// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.graphql.inbound

import com.expediagroup.graphql.generator.annotations.GraphQLDescription
import com.expediagroup.graphql.server.operations.Query
import kotlinx.serialization.Serializable
import org.eclipse.lmos.arc.agents.AgentProvider

/**
 * Returns the list of available Agents.
 */
class AgentQuery(private val agentProvider: AgentProvider) : Query {

    @GraphQLDescription("Returns the list of available Agents.")
    fun agent(): Agents {
        return Agents(agentProvider.getAgents().map { it.name })
    }
}

@Serializable
data class Agents(val names: List<String>)
