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
import org.eclipse.lmos.arc.agents.events.EventListeners
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionLoader
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ListFunctionsLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
import org.eclipse.lmos.arc.agents.llm.ChatCompleter
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ServiceCompleterProvider
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.agents.tracing.AgentTracer

/**
 * Interface for an Arc agent system.
 */
interface ArcAgents : AgentProvider, LLMFunctionProvider, EventListeners

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
            functionLoaders: List<LLMFunctionLoader> = emptyList(),
        ): DSLAgents {
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

            return DSLAgents(
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

    override fun add(handler: EventHandler<out Event>) {
        eventListeners?.add(handler) // TODO
    }
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
 *
 * @param chatCompleterProvider The ChatCompleterProvider to use.
 * @param functionLoaders Additional function loaders to use.
 * @param memory The memory to use, default is InMemoryMemory.
 * @param eventPublisher The event publisher to use. If set then the handlers will be ignored.
 * @param tracer The tracer to use.
 * @param context A collection of beans to be accessed in the agent DSL.
 * @param handlers The list of event handlers that are assigned to the EventPublisher.
 * @param functions The function used to define tools.
 * @param builder The function used to define agents.
 */
fun agents(
    tracer: AgentTracer? = null,
    eventPublisher: EventPublisher? = null,
    chatCompleterProvider: ChatCompleterProvider? = null,
    functionLoaders: List<LLMFunctionLoader> = emptyList(),
    memory: Memory? = InMemoryMemory(),
    context: Set<Any> = emptySet(),
    handlers: List<EventHandler<out Event>> = emptyList(),
    functions: FunctionDefinitionContext.() -> Unit = {},
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
