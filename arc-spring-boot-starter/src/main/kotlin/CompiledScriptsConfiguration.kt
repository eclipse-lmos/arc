// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import org.eclipse.lmos.arc.agents.AgentLoader
import org.eclipse.lmos.arc.agents.dsl.BasicAgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean

@ConditionalOnProperty("arc.scripts.enabled", havingValue = "false")
class CompiledScriptsConfiguration {

    @Bean
    @ConditionalOnClass(org.eclipse.lmos.arc.agents.gen.Agents::class)
    fun agents(agentFactory: ChatAgentFactory): AgentLoader {
        val context = BasicAgentDefinitionContext(agentFactory)
        with(context) {
            org.eclipse.lmos.arc.agents.gen.Agents().build()
        }
        val agents = context.agents.toList()
        return AgentLoader { agents }
    }

    @Bean
    @ConditionalOnClass(org.eclipse.lmos.arc.agents.gen.Functions::class)
    fun functions(beanProvider: BeanProvider): LLMFunctionLoader {
        val context = BasicFunctionDefinitionContext(beanProvider)
        with(context) {
            org.eclipse.lmos.arc.agents.gen.Functions().build()
        }
        val functions = context.functions.toList()
        return LLMFunctionLoader { functions }
    }
}
