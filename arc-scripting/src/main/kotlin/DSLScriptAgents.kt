// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.CompositeAgentProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CompositeBeanProvider
import org.eclipse.lmos.arc.agents.dsl.beans
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import org.eclipse.lmos.arc.scripting.agents.ScriptingAgentLoader
import org.eclipse.lmos.arc.scripting.functions.ScriptingLLMFunctionLoader

/**
 * A convenience class for setting up the agent system and
 * defining agents and functions in a DSL.
 */
class DSLScriptAgents private constructor(
    private val functionsLoader: ScriptingLLMFunctionLoader,
    private val agentLoader: ScriptingAgentLoader,
    private val agentProvider: AgentProvider,
    private val functionProvider: LLMFunctionProvider,
) : AgentProvider, LLMFunctionProvider {
    companion object {

        fun init(
            chatCompleterProvider: ChatCompleterProvider,
            beans: Set<Any> = emptySet(),
            handlers: List<EventHandler<out Event>> = emptyList(),
        ): DSLScriptAgents {
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
            val functionLoader = ScriptingLLMFunctionLoader(beanProvider, eventPublisher = eventPublisher)
            val functionProvider = CompositeLLMFunctionProvider(listOf(functionLoader))

            /**
             * Set up the loading of agents from scripts.
             */
            val agentFactory = ChatAgentFactory(CompositeBeanProvider(setOf(functionProvider), beanProvider))
            val agentLoader = ScriptingAgentLoader(agentFactory, eventPublisher = eventPublisher)
            val agentProvider = CompositeAgentProvider(listOf(agentLoader), emptyList())

            return DSLScriptAgents(functionLoader, agentLoader, agentProvider, functionProvider)
        }
    }

    /**
     * Define agents.
     */
    fun define(agentDSLScript: String) = result<Int, ScriptFailedException> {
        agentLoader.loadAgent(agentDSLScript) failWith { it }
        agentLoader.getAgents().size
    }

    /**
     * Define functions.
     */
    fun defineFunctions(functionDSLScript: String) = result<Int, ScriptFailedException> {
        functionsLoader.loadFunction(functionDSLScript) failWith { it }
        functionsLoader.load().size
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
