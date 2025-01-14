// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.runner.server

import org.eclipse.lmos.arc.agents.AgentProvider
import org.eclipse.lmos.arc.agents.CompositeAgentProvider
import org.eclipse.lmos.arc.agents.dsl.ChatAgentFactory
import org.eclipse.lmos.arc.agents.dsl.CompositeBeanProvider
import org.eclipse.lmos.arc.agents.dsl.beans
import org.eclipse.lmos.arc.agents.events.BasicEventPublisher
import org.eclipse.lmos.arc.agents.events.LoggingEventHandler
import org.eclipse.lmos.arc.agents.functions.CompositeLLMFunctionProvider
import org.eclipse.lmos.arc.graphql.inbound.EventSubscriptionHolder
import org.eclipse.lmos.arc.scripting.ScriptHotReload
import org.eclipse.lmos.arc.scripting.agents.ScriptingAgentLoader
import org.eclipse.lmos.arc.scripting.functions.ScriptingLLMFunctionLoader
import kotlin.time.Duration.Companion.seconds

/**
 * Initializes the ARC Framework.
 */
fun setupArc(appConfig: AppConfig): Pair<AgentProvider, EventSubscriptionHolder> {
    /**
     * Set up the event system.
     */
    val eventSubscriptionHolder = EventSubscriptionHolder()
    val eventPublisher = BasicEventPublisher(LoggingEventHandler(), eventSubscriptionHolder)

    /**
     * Set up the chat completer and the bean provider.
     */
    val chatCompleterProvider = chatCompleterProvider(appConfig.clientConfig, eventPublisher)
    val beanProvider = beans(chatCompleterProvider, eventPublisher)

    /**
     * Set up the loading of agent functions from scripts.
     */
    val functionLoader = ScriptingLLMFunctionLoader(beanProvider, eventPublisher = eventPublisher)
    functionLoader.loadAgentsFromFolder(appConfig.scriptFolder)
    val functionProvider = CompositeLLMFunctionProvider(listOf(functionLoader))

    /**
     * Set up the loading of agents from scripts.
     */
    val agentFactory = ChatAgentFactory(CompositeBeanProvider(setOf(functionProvider), beanProvider))
    val agentLoader = ScriptingAgentLoader(agentFactory, eventPublisher = eventPublisher)
    agentLoader.loadAgentsFromFolder(appConfig.scriptFolder)
    val agentProvider = CompositeAgentProvider(listOf(agentLoader), emptyList())

    /**
     * Set up hot-reload for agents and functions.
     */
    val scriptHotReload = ScriptHotReload(agentLoader, functionLoader, 3.seconds)
    scriptHotReload.start(appConfig.scriptFolder)

    return agentProvider to eventSubscriptionHolder
}
