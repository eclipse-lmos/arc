// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
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
