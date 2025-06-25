// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.agent.Skill
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
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
    var version: String = "1.0.0"
    var onFlags: Set<String> = emptySet()

    var skills: suspend () -> List<Skill> = { emptyList() }
    fun skills(fn: suspend () -> List<Skill>) {
        skills = fn
    }

    var model: suspend DSLContext.() -> String? = { null }
    fun model(fn: suspend DSLContext.() -> String) {
        model = fn
    }

    var settings: suspend DSLContext.() -> ChatCompletionSettings? = { null }
    fun settings(fn: suspend DSLContext.() -> ChatCompletionSettings) {
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
        val previous = outputFilter
        outputFilter = {
            previous()
            fn()
        }
    }

    var inputFilter: suspend InputFilterContext.() -> Unit = { }
    fun filterInput(fn: suspend InputFilterContext.() -> Unit) {
        val previous = inputFilter
        inputFilter = {
            previous()
            fn()
        }
    }

    var init: DSLContext.() -> Unit = { }
    fun init(fn: DSLContext.() -> Unit) {
        init = fn
    }

    var onFail: suspend DSLContext.(Exception) -> AssistantMessage? = { null }
    fun onFail(fn: suspend DSLContext.(Exception) -> AssistantMessage?) {
        onFail = fn
    }
}
