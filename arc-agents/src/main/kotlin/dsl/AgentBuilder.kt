// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.functions.LLMFunction

/**
 * Handy function to builds agents using the given [agentFactory] and [builder].
 * @return the list of agents created.
 */
fun buildAgents(agentFactory: AgentFactory<*>, builder: AgentDefinitionContext.() -> Unit): List<Agent<*, *>> {
    val context = BasicAgentDefinitionContext(agentFactory)
    with(context) {
        builder()
    }
    return context.agents.toList()
}

/**
 * Handy function to builds agent functions.
 * @return the list of functions created.
 */
fun buildFunctions(beanProvider: BeanProvider, builder: BasicFunctionDefinitionContext.() -> Unit): List<LLMFunction> {
    val context = BasicFunctionDefinitionContext(beanProvider)
    with(context) {
        builder()
    }
    return context.functions.toList()
}
