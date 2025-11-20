// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

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
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.mcp.McpPromptRetriever
import org.eclipse.lmos.arc.mcp.McpTools
import org.eclipse.lmos.arc.spring.clients.ClientsConfiguration
import org.eclipse.lmos.arc.spring.inbound.AgentCardController
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
@Import(
    MetricConfiguration::class,
    TracingConfiguration::class,
    ClientsConfiguration::class,
    ScriptingConfiguration::class,
    CompiledScriptsConfiguration::class,
    McpConfiguration::class,
)
open class ArcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BeanProvider::class)
    open fun beanProvider(beanFactory: ConfigurableBeanFactory): BeanProvider = CoroutineBeanProvider(object : BeanProvider {
        override suspend fun <T : Any> provide(bean: KClass<T>) = try {
            beanFactory.getBean(bean.java)
        } catch (e: NoSuchBeanDefinitionException) {
            throw MissingBeanException("Bean of type $bean cannot be located!")
        }
    })

    @Bean
    @ConditionalOnProperty("arc.mcp.prompts.url")
    open fun mcpPromptRetriever(@Value("\${arc.mcp.prompts.url}") url: String): PromptRetriever = McpPromptRetriever(url)

    @Bean
    open fun eventPublisher(eventHandlers: List<EventHandler<*>>) = BasicEventPublisher().apply {
        addAll(eventHandlers)
    }

    @Bean
    @ConditionalOnMissingBean(AgentFactory::class)
    open fun agentFactory(beanProvider: BeanProvider) = ChatAgentFactory(beanProvider)

    @Bean
    @ConditionalOnMissingBean(Memory::class)
    open fun memory() = InMemoryMemory()

    @Bean
    open fun loggingEventHandler() = LoggingEventHandler()

    @Bean
    open fun agentCardController(agentProvider: AgentProvider) = AgentCardController(agentProvider)

    @Bean
    @ConditionalOnMissingBean(AgentProvider::class)
    open fun agentProvider(loaders: List<AgentLoader>, agents: List<Agent<*, *>>): AgentProvider =
        CompositeAgentProvider(loaders, agents)

    @Bean
    @ConditionalOnMissingBean(LLMFunctionProvider::class)
    open fun llmFunctionProvider(
        loaders: List<LLMFunctionLoader>,
        functions: List<LLMFunction>,
        @Value("\${arc.mcp.tools.urls:}") urls: List<String>? = null,
        @Value("\${arc.mcp.tools.cache.duration:}") cacheDuration: Duration? = null,
    ): LLMFunctionProvider =
        CompositeLLMFunctionProvider(
            loaders + (urls?.map { url -> McpTools(url, cacheDuration) } ?: emptyList()),
            functions,
        )

    @Bean
    open fun agentLoader(agentFactory: AgentFactory<*>) = Agents(agentFactory)

    @Bean
    open fun functionLoader(beanProvider: BeanProvider) = Functions(beanProvider)
}
