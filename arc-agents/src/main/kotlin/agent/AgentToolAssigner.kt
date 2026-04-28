// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.agent

import org.eclipse.lmos.arc.agents.dsl.DSLContext

/**
 * Assigns the tools available to an agent at runtime.
 */
interface AgentToolAssigner {

    /**
     * Returns the tool names that should be available to the agent.
     */
    suspend fun assignTools(agentName: String, currentTools: Set<String>, context: DSLContext): Set<String>
}
