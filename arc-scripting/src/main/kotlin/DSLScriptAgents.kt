// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.scripting

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.AgentLoader
import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.ArcAgents
import org.eclipse.lmos.arc.agents.CompositeAgentProvider
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
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.LLMFunctionServiceLoader
import org.eclipse.lmos.arc.agents.functions.ToolLoaderContext
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ServiceCompleterProvider
import org.eclipse.lmos.arc.agents.memory.InMemoryMemory
import org.eclipse.lmos.arc.agents.memory.Memory
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.result
import org.eclipse.lmos.arc.scripting.agents.ScriptingAgentLoader
import org.eclipse.lmos.arc.scripting.functions.ScriptingLLMFunctionLoader
import org.slf4j.LoggerFactory
import java.io.File
import kotlin.reflect.KClass
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

        private val log = LoggerFactory.getLogger(DSLScriptAgents::class.java)

        fun init(
            chatCompleterProvider: ChatCompleterProvider? = null,
            beans: Set<Any> = emptySet(),
            publisher: EventPublisher? = null,
            handlers: List<EventHandler<out Event>> = emptyList(),
            memory: Memory? = null,
            tracer: AgentTracer? = null,
            scriptFolder: File? = null,
            hotReload: Boolean = false,
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
            if (scriptFolder != null) functionLoader.loadFunctionsFromFolder(scriptFolder)
            val discoveredLoaders = LLMFunctionServiceLoader()
            val functionProvider = CompositeLLMFunctionProvider(listOf(functionLoader, discoveredLoaders))

            /**
             * Set up the loading of agents from scripts.
             */
            val agentLoaders = mutableListOf<AgentLoader>()
            val agentProvider = CompositeAgentProvider(agentLoaders, emptyList())
            val agentFactory =
                ChatAgentFactory(CompositeBeanProvider(setOf(functionProvider, agentProvider), beanProvider))
            val agentLoader = ScriptingAgentLoader(agentFactory, eventPublisher = eventPublisher)
            if (scriptFolder != null) agentLoader.loadAgentsFromFolder(scriptFolder)
            agentLoaders.add(agentLoader)

            /**
             * Set up hot-reload for agents and functions.
             */
            if (hotReload && scriptFolder != null) {
                val scriptHotReload = ScriptHotReload(agentLoader, functionLoader, 3.seconds)
                scriptHotReload.start(scriptFolder)
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

    override suspend fun <T : Any> provide(bean: KClass<T>): T {
        return beanProvider.provide(bean)
    }

    override fun add(handler: EventHandler<out Event>) {
        eventListeners?.add(handler) // TODO
    }
}

/**
 * Creates a DSLScriptAgents instance with hot reloading capabilities.
 *
 * This function initializes a DSLScriptAgents instance that can automatically reload agent and function scripts
 * when they change in the specified directory. It uses the ScriptHotReload class to watch for file changes
 * and reload scripts accordingly.
 *
 * @param context A set of objects that will be available as beans in the agent system
 * @param memory Optional memory implementation for agents to use
 * @param tracer Optional tracer for monitoring agent execution
 * @param folder The directory to watch for script changes. If null, hot reloading is disabled
 * @param chatCompleterProvider Optional custom chat completer provider. If null, a default ServiceCompleterProvider is used
 * @return A configured DSLScriptAgents instance with hot reloading if a folder is specified
 */
fun hotReloadAgents(
    folder: File,
    context: Set<Any> = emptySet(),
    memory: Memory? = InMemoryMemory(),
    tracer: AgentTracer? = null,
    chatCompleterProvider: ChatCompleterProvider? = null,
): DSLScriptAgents {
    return DSLScriptAgents.init(
        chatCompleterProvider = chatCompleterProvider,
        memory = memory,
        beans = context,
        tracer = tracer,
        scriptFolder = folder,
        hotReload = true,
    )
}

fun loadAgentsFrom(
    folder: File,
    context: Set<Any> = emptySet(),
    memory: Memory? = InMemoryMemory(),
    tracer: AgentTracer? = null,
    chatCompleterProvider: ChatCompleterProvider? = null,
): DSLScriptAgents {
    return DSLScriptAgents.init(
        chatCompleterProvider = chatCompleterProvider,
        memory = memory,
        beans = context,
        tracer = tracer,
        scriptFolder = folder,
        hotReload = false,
    )
}
