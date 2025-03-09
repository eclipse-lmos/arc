// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.spring

import com.azure.ai.openai.OpenAIAsyncClient
import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentLoader
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.CompositeAgentProvider
import org.eclipse.lmos.arc.agents.dsl.AgentFactory
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CoroutineBeanProvider
import org.eclipse.lmos.arc.agents.dsl.MissingBeanException
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
import org.eclipse.lmos.arc.spring.clients.AzureOpenAIConfiguration
import org.eclipse.lmos.arc.spring.clients.BedrockConfiguration
import org.eclipse.lmos.arc.spring.clients.ClientsConfiguration
import org.eclipse.lmos.arc.spring.clients.GeminiConfiguration
import org.eclipse.lmos.arc.spring.clients.GroqConfiguration
import org.eclipse.lmos.arc.spring.clients.OllamaConfiguration
import org.eclipse.lmos.arc.spring.clients.OpenAIConfiguration
import org.springframework.beans.factory.NoSuchBeanDefinitionException
import org.springframework.beans.factory.annotation.Value
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import kotlin.reflect.KClass

@AutoConfiguration
@Import(
    MetricConfiguration::class,
    TracingConfiguration::class,
    ClientsConfiguration::class,
    ScriptingConfiguration::class,
    CompiledScriptsConfiguration::class,
)
open class ArcAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BeanProvider::class)
    fun beanProvider(beanFactory: ConfigurableBeanFactory): BeanProvider = CoroutineBeanProvider(object : BeanProvider {
        override suspend fun <T : Any> provide(bean: KClass<T>) = try {
            beanFactory.getBean(bean.java)
        } catch (e: NoSuchBeanDefinitionException) {
            throw MissingBeanException("Bean of type $bean cannot be located!")
        }
    })

    @Bean
    @ConditionalOnProperty("arc.mcp.prompts.url")
    fun mcpPromptRetriever(@Value("\${arc.mcp.prompts.url}") url: String) = McpPromptRetriever(url)

    @Bean
    fun eventPublisher(eventHandlers: List<EventHandler<*>>) = BasicEventPublisher().apply {
        addAll(eventHandlers)
    }

    @Bean
    @ConditionalOnMissingBean(AgentFactory::class)
    fun agentFactory(beanProvider: BeanProvider) = ChatAgentFactory(beanProvider)

    @Bean
    @ConditionalOnMissingBean(Memory::class)
    fun memory() = InMemoryMemory()

    @Bean
    fun loggingEventHandler() = LoggingEventHandler()

    @Bean
    @ConditionalOnProperty("arc.router.enable", havingValue = "true")
    fun semanticRouter(
        @Value("\${arc.router.model}") model: String,
        textEmbedderProvider: TextEmbedderProvider,
        agentProvider: AgentProvider,
        initialRoutes: SemanticRoutes? = null,
        eventPublisher: EventPublisher,
    ): SemanticRouter {
        return SemanticRouter(textEmbedderProvider.provideByModel(model), initialRoutes, eventPublisher)
    }

    @Bean
    @ConditionalOnMissingBean(AgentProvider::class)
    fun agentProvider(loaders: List<AgentLoader>, agents: List<Agent<*, *>>): AgentProvider =
        CompositeAgentProvider(loaders, agents)

    @Bean
    fun llmFunctionProvider(loaders: List<LLMFunctionLoader>, functions: List<LLMFunction>): LLMFunctionProvider =
        CompositeLLMFunctionProvider(loaders, functions)

    @Bean
    fun agentLoader(agentFactory: AgentFactory<*>) = Agents(agentFactory)

    @Bean
    fun functionLoader(beanProvider: BeanProvider) = Functions(beanProvider)
}
