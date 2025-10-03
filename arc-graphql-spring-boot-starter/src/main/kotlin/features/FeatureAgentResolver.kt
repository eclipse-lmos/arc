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

    /**
     * Resolves an agent based on the provided agent name and request.
     * It checks the features of each agent and returns the first one that matches the enabled features.
     *
     * @param agentName The name of the agent to resolve (not used in this implementation).
     * @param request The agent request containing context information.
     * @return The resolved agent or null if no suitable agent is found.
     */
    override fun resolveAgent(
        agentName: String?,
        request: AgentRequest,
    ): Agent<*, *>? {
        return agentProvider.getAgents().firstOrNull { agent ->
            agent.activateOnFeatures?.isNotEmpty() == true && agent.activateOnFeatures?.all {
                features.getFeatureBoolean(
                    it,
                )
            } == true
        } ?: agentProvider.getAgents().firstOrNull { agent ->
            agent.activateOnFeatures == null || agent.activateOnFeatures?.isEmpty() == true
        }
    }
}
