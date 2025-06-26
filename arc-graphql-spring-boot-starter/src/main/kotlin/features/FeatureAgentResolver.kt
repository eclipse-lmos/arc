// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package features

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.features.FeatureFlags
import org.eclipse.lmos.arc.api.AgentRequest
import org.eclipse.lmos.arc.graphql.AgentResolver

/**
 * Resolves the agent based on feature flags.
 * Returns the first agent that has all its feature flags enabled.
 */
class FeatureAgentResolver(
    private val features: FeatureFlags,
    private val agentProvider: AgentProvider,
) : AgentResolver {

    override fun resolveAgent(
        agentName: String?,
        request: AgentRequest,
    ): Agent<*, *>? {
        return agentProvider.getAgents().firstOrNull { agent ->
            agent.onFeatures.isNotEmpty() && agent.onFeatures.all { features.isFeatureEnabled(it) }
        }
    }
}
