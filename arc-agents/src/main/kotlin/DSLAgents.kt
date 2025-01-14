// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import org.eclipse.lmos.arc.agents.dsl.AgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BasicAgentDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BasicFunctionDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CompositeBeanProvider
import org.eclipse.lmos.arc.agents.dsl.FunctionDefinitionContext
import org.eclipse.lmos.arc.agents.dsl.beans
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ListFunctionsLoader
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider

/**
 * A convenience class for setting up the agent system and
 * defining agents and functions in a DSL.
 */
class DSLAgents private constructor(
    private val beanProvider: BeanProvider,
    private val agentFactory: ChatAgentFactory,
    private val functionsLoader: ListFunctionsLoader,
    private val agentLoader: ListAgentLoader,
    private val agentProvider: AgentProvider,
    private val functionProvider: LLMFunctionProvider,
) : AgentProvider, LLMFunctionProvider {
    companion object {

        fun init(
            chatCompleterProvider: ChatCompleterProvider,
            beans: Set<Any> = emptySet(),
            handlers: List<EventHandler<out Event>> = emptyList(),
        ): DSLAgents {
            /**
             * Set up the event system.
             */
            val eventPublisher = BasicEventPublisher(LoggingEventHandler(), *handlers.toTypedArray())

            /**
             * Set up the bean provider.
             */
            val beanProvider = beans(chatCompleterProvider, eventPublisher, *beans.toTypedArray())

            /**
             * Set up the loading of agent functions from scripts.
             */
            val functionLoader = ListFunctionsLoader()
            val functionProvider = CompositeLLMFunctionProvider(listOf(functionLoader))

            /**
             * Set up the loading of agents from scripts.
             */
            val agentFactory = ChatAgentFactory(CompositeBeanProvider(setOf(functionProvider), beanProvider))
            val agentLoader = ListAgentLoader()
            val agentProvider = CompositeAgentProvider(listOf(agentLoader), emptyList())

            return DSLAgents(beanProvider, agentFactory, functionLoader, agentLoader, agentProvider, functionProvider)
        }
    }

    /**
     * Define agents.
     */
    fun define(agentBuilder: AgentDefinitionContext.() -> Unit) {
        val context = BasicAgentDefinitionContext(agentFactory)
        with(context) {
            agentBuilder()
        }
        val agents = context.agents.toList()
        agentLoader.addAll(agents)
    }

    /**
     * Define functions.
     */
    fun defineFunctions(functionBuilder: FunctionDefinitionContext.() -> Unit) {
        val context = BasicFunctionDefinitionContext(beanProvider)
        with(context) {
            functionBuilder()
        }
        val functions = context.functions.toList()
        functionsLoader.addAll(functions)
    }

    /**
     * Get agents.
     */
    override fun getAgents(): List<Agent<*, *>> = agentProvider.getAgents()

    /**
     * Get functions.
     */
    override fun provide(functionName: String) = functionProvider.provide(functionName)

    override fun provideAll() = functionProvider.provideAll()
}
