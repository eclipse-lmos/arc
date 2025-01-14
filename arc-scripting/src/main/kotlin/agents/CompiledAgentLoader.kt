// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting.agents

import org.eclipse.lmos.arc.agents.dsl.AgentDefinitionContext

/**
 * Interface for loading compiled agents.
 */
interface CompiledAgentLoader {

    fun load(agentDefinitionContext: AgentDefinitionContext)
}
