// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ArcAgents
import org.eclipse.lmos.arc.agents.CompositeAgentProvider
import org.eclipse.lmos.arc.agents.DSLAgents
import org.eclipse.lmos.arc.agents.ListAgentLoader
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CompositeBeanProvider
import org.eclipse.lmos.arc.agents.dsl.beans
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.events.EventListeners
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ListFunctionsLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ServiceCompleterProvider
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import org.eclipse.lmos.arc.scripting.agents.ScriptingAgentLoader
import org.eclipse.lmos.arc.scripting.functions.ScriptingLLMFunctionLoader
import java.io.File
import kotlin.time.Duration.Companion.seconds

/**
 * A convenience class for setting up the agent system and
 * defining agents and functions in a DSL.
 */
class DSLScriptAgents private constructor(
    private val beanProvider: BeanProvider,
    private val agentFactory: ChatAgentFactory,
    private val functionsLoader: ScriptingLLMFunctionLoader,
    private val agentLoader: ScriptingAgentLoader,
    private val agentProvider: AgentProvider,
    private val functionProvider: LLMFunctionProvider,
    private val eventListeners: EventListeners? = null,
) : ArcAgents {
    companion object {

        fun init(
            chatCompleterProvider: ChatCompleterProvider? = null,
            beans: Set<Any> = emptySet(),
            publisher: EventPublisher? = null,
            handlers: List<EventHandler<out Event>> = emptyList(),
            memory: Memory? = null,
            tracer: AgentTracer? = null,
            hotReloadFolder: File? = null,
        ): DSLScriptAgents {
            /**
             * Set up the event system.
             */
            val eventPublisher = publisher ?: BasicEventPublisher(LoggingEventHandler(), *handlers.toTypedArray())

            /**
             * Set up the chat completer provider.
             */
            val completerProvider =
                chatCompleterProvider ?: ServiceCompleterProvider(tracer = tracer, eventPublisher = eventPublisher)

            /**
             * Set up the bean provider.
             */
            val beanProvider = beans(*beans.toTypedArray(), completerProvider, memory, tracer, eventPublisher)

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

            /**
             * Set up hot-reload for agents and functions.
             */
            if (hotReloadFolder != null) {
                val scriptHotReload = ScriptHotReload(agentLoader, functionLoader, 3.seconds)
                scriptHotReload.start(hotReloadFolder)
            }

            return DSLScriptAgents(
                beanProvider,
                agentFactory,
                functionLoader,
                agentLoader,
                agentProvider,
                functionProvider,
                if (eventPublisher is EventListeners) eventPublisher else null,
            )
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
    suspend fun defineFunctions(functionDSLScript: String) = result<Int, ScriptFailedException> {
        functionsLoader.loadFunction(functionDSLScript) failWith { it }
        functionsLoader.load(null).size
    }

    /**
     * Get agents.
     */
    override fun getAgents(): List<Agent<*, *>> = agentProvider.getAgents()

    /**
     * Get functions.
     */
    override suspend fun provide(functionName: String, context: ToolLoaderContext?) =
        functionProvider.provide(functionName, context)

    override suspend fun provideAll(context: ToolLoaderContext?) = functionProvider.provideAll(context)

    override fun add(handler: EventHandler<out Event>) {
        eventListeners?.add(handler) // TODO
    }
}


fun hotReloadAgents(
    context: Set<Any> = emptySet(),
    memory: Memory? = null,
    tracer: AgentTracer? = null,
    hotReloadFolder: File? = null,
    chatCompleterProvider: ChatCompleterProvider? = null,
): DSLScriptAgents {
    return DSLScriptAgents.init(
        chatCompleterProvider = chatCompleterProvider,
        memory = memory,
        beans = context,
        tracer = tracer,
        hotReloadFolder = hotReloadFolder,
    )
}