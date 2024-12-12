// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings

@DslMarker
annotation class AgentDefinitionContextMarker

@AgentDefinitionContextMarker
interface AgentDefinitionContext {

    fun agent(agent: AgentDefinition.() -> Unit)
}

/**
 * Used as an implicit receiver for agent scripts.
 */
class BasicAgentDefinitionContext(
    private val agentFactory: AgentFactory<*>,
) : AgentDefinitionContext {

    val agents = mutableListOf<Agent<*, *>>()

    override fun agent(agent: AgentDefinition.() -> Unit) {
        val agentDefinition = AgentDefinition()
        agent.invoke(agentDefinition)
        agents.add(agentFactory.createAgent(agentDefinition))
    }
}

class AgentDefinition {
    lateinit var name: String
    var description: String = ""

    var model: () -> String? = { null }
    fun model(fn: () -> String) {
        model = fn
    }

    var settings: () -> ChatCompletionSettings? = { null }
    fun settings(fn: () -> ChatCompletionSettings) {
        settings = fn
    }

    private var _toolsProvider: suspend DSLContext.() -> Unit = { tools.forEach { +it } }
    val toolsProvider get() = _toolsProvider

    var tools: List<String> = emptyList()
    fun tools(fn: suspend DSLContext.() -> Unit) {
        _toolsProvider = {
            tools.forEach { +it }
            fn()
        }
    }

    var systemPrompt: suspend DSLContext.() -> String = { "" }
        get() = {
            val result = field()
            if (this is BasicDSLContext) {
                (output.get() + result).trimIndent()
            } else {
                result.trimIndent()
            }
        }

    fun prompt(fn: suspend DSLContext.() -> String) {
        systemPrompt = fn
    }

    var outputFilter: suspend OutputFilterContext.() -> Unit = { }
    fun filterOutput(fn: suspend OutputFilterContext.() -> Unit) {
        outputFilter = fn
    }

    var inputFilter: suspend InputFilterContext.() -> Unit = { }
    fun filterInput(fn: suspend InputFilterContext.() -> Unit) {
        inputFilter = fn
    }
}
