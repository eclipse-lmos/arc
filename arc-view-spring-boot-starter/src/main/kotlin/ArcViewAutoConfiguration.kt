// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.view

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentLoader
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.CompositeAgentProvider
import org.eclipse.lmos.arc.agents.dsl.AgentFactory
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CoroutineBeanProvider
import org.eclipse.lmos.arc.agents.dsl.MissingBeanException
import org.eclipse.lmos.arc.agents.dsl.extensions.PromptRetriever
import org.eclipse.lmos.arc.agents.events.*
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.llm.TextEmbedderProvider
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.agents.router.SemanticRouter
import org.eclipse.lmos.arc.agents.router.SemanticRoutes
import org.eclipse.lmos.arc.mcp.McpPromptRetriever
import org.eclipse.lmos.arc.mcp.McpTools
import org.eclipse.lmos.arc.spring.clients.ClientsConfiguration
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import java.time.Duration
import kotlin.reflect.KClass

@AutoConfiguration
open class ArcViewAutoConfiguration {

    @Bean
    fun chatResourceRouter() = RouterFunctions.resources("/chat/**", ClassPathResource("chat/"))
}
