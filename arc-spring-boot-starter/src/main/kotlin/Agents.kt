// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.spring

import ai.ancf.lmos.arc.agents.Agent
import ai.ancf.lmos.arc.agents.dsl.AgentDefinition
import ai.ancf.lmos.arc.agents.dsl.AgentFactory
import ai.ancf.lmos.arc.agents.dsl.BasicAgentDefinitionContext

/**
 * A factory for creating agents using the Arc Agent DSL.
 * Mainly used for creating Agents inside of Configuration classes.
 *
 * For example:
 * @Bean
 * fun myAgent(agent: Agents) = agent {
 *   name = "My Agent"
 *   systemPrompt = { "you are a helpful agent that tell funny jokes." }
 * }
 */
class Agents(private val agentFactory: AgentFactory<*>) {

    operator fun invoke(agentDefinition: AgentDefinition.() -> Unit): Agent<*, *> {
        val context = BasicAgentDefinitionContext(agentFactory)
        context.agent { agentDefinition() }
        return context.agents.first()
    }
}
