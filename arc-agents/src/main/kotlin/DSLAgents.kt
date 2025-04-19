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
import org.eclipse.lmos.arc.agents.env.EnvironmentCompleterProvider
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.Event
import org.eclipse.lmos.arc.agents.events.EventHandler
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ListFunctionsLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.agents.tracing.AgentTracer

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
            publisher: EventPublisher? = null,
            handlers: List<EventHandler<out Event>> = emptyList(),
            memory: Memory? = null,
            tracer: AgentTracer? = null,
            functionLoaders: List<LLMFunctionLoader> = emptyList(),
        ): DSLAgents {
            /**
             * Set up the event system.
             */
            val eventPublisher = publisher ?: BasicEventPublisher(LoggingEventHandler(), *handlers.toTypedArray())

            /**
             * Set up the bean provider.
             */
            val beanProvider = beans(*beans.toTypedArray(), chatCompleterProvider, memory, tracer, eventPublisher)

            /**
             * Set up the loading of agent functions.
             */
            val functionLoader = ListFunctionsLoader()
            val functionProvider = CompositeLLMFunctionProvider(functionLoaders + functionLoader)

            /**
             * Set up the loading of agents.
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
    fun define(agentBuilder: AgentDefinitionContext.() -> Unit): DSLAgents {
        val context = BasicAgentDefinitionContext(agentFactory)
        with(context) {
            agentBuilder()
        }
        val agents = context.agents.toList()
        agentLoader.addAll(agents)
        return this
    }

    /**
     * Define functions.
     */
    fun defineFunctions(functionBuilder: FunctionDefinitionContext.() -> Unit): DSLAgents {
        val context = BasicFunctionDefinitionContext(beanProvider)
        with(context) {
            functionBuilder()
        }
        val functions = context.functions.toList()
        functionsLoader.addAll(functions)
        return this
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
}

/**
 * Set up the agent system with a single AI Client / ChatCompleter.
 */
@Deprecated(
    "Use the agents function instead. This function is deprecated and will be removed in a future version.",
    ReplaceWith("agents"),
)
fun arcAgents(chatCompleter: ChatCompleter, beans: Set<Any> = emptySet()): DSLAgents {
    val chatCompleterProvider = ChatCompleterProvider { chatCompleter }
    return DSLAgents.init(chatCompleterProvider, beans)
}

/**
 * Get a ChatAgent by name.
 */
fun DSLAgents.getChatAgent(name: String) = getAgents().find { it.name == name } as ConversationAgent

/**
 * Convenience function to set up the Arc agent system.
 */
fun agents(
    functions: FunctionDefinitionContext.() -> Unit = {},
    chatCompleterProvider: ChatCompleterProvider = EnvironmentCompleterProvider(),
    functionLoaders: List<LLMFunctionLoader> = emptyList(),
    memory: Memory? = InMemoryMemory(),
    eventPublisher: EventPublisher? = null,
    tracer: AgentTracer? = null,
    context: Set<Any> = emptySet(),
    handlers: List<EventHandler<out Event>> = emptyList(),
    builder: AgentDefinitionContext.() -> Unit = {},
): DSLAgents {
    return DSLAgents.init(
        chatCompleterProvider = chatCompleterProvider,
        beans = context,
        publisher = eventPublisher,
        handlers = handlers,
        memory = memory,
        tracer = tracer,
        functionLoaders = functionLoaders,
    ).defineFunctions {
        functions()
    }.define {
        builder()
    }
}
