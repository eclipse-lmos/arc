// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.agents.dsl

import ai.ancf.lmos.arc.agents.Agent
import ai.ancf.lmos.arc.agents.ChatAgent

/**
 * Factory for creating agents from Agent Definitions.
 */
fun interface AgentFactory<T : Agent<*, *>> {
    fun createAgent(agentDefinition: AgentDefinition): T
}

/**
 * Default implementation of [AgentFactory] that create [ChatAgent] instances from [AgentDefinition]s.
 */
class ChatAgentFactory(private val beanProvider: BeanProvider) : AgentFactory<ChatAgent> {

    override fun createAgent(agentDefinition: AgentDefinition): ChatAgent {
        return ChatAgent(
            name = agentDefinition.name,
            description = agentDefinition.description,
            model = agentDefinition.model,
            agentDefinition.settings,
            beanProvider,
            agentDefinition.systemPrompt,
            agentDefinition.tools,
            agentDefinition.outputFilter,
            agentDefinition.inputFilter,
        )
    }
}
