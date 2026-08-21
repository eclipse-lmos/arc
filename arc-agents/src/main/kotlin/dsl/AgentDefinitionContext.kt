// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents.dsl

import org.eclipse.lmos.arc.agents.Agent
import org.eclipse.lmos.arc.agents.agent.Skill
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.OutputFormat
import org.eclipse.lmos.arc.agents.llm.OutputSchema
import kotlin.reflect.KClass

@DslMarker
annotation class AgentDefinitionContextMarker

@AgentDefinitionContextMarker
interface AgentDefinitionContext {

    val agent: AgentDefinitionBuilder
}

class AgentDefinitionBuilder(private val register: (AgentDefinition.() -> Unit) -> Unit) {

    operator fun invoke(configure: AgentDefinition.() -> Unit) {
        register(configure)
    }

    @JvmName("invokeTyped")
    inline operator fun <reified Input : Any, reified Output : Any> invoke(
        noinline configure: AgentDefinition.() -> Unit,
    ) {
        invoke {
            inputType = Input::class
            output<Output>()
            configure()
        }
    }
}

/**
 * Used as an implicit receiver for agent scripts.
 */
class BasicAgentDefinitionContext(
    private val agentFactory: AgentFactory<*>,
) : AgentDefinitionContext {

    val agents = mutableListOf<Agent<*, *>>()

    override val agent = AgentDefinitionBuilder { configure ->
        val agentDefinition = AgentDefinition()
        configure.invoke(agentDefinition)
        agents.add(agentFactory.createAgent(agentDefinition))
    }
}

class AgentDefinition {
    lateinit var name: String
    var description: String = ""
    var version: String = "1.0.0"
    var activateOnFeatures: Set<String> = emptySet()
    var inputType: KClass<*>? = null
    var outputType: KClass<*>? = null

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

    inline fun <reified T> output(
        name: String = "",
        description: String = "",
        temperature: Double? = null,
        seed: Long? = null,
    ) {
        outputType = T::class
        val previous = settings
        settings = {
            (previous() ?: ChatCompletionSettings()).copy(
                outputSchema = OutputSchema(
                    name = name,
                    description = description,
                    type = T::class,
                ),
                format = OutputFormat.JSON,
                temperature = temperature,
                seed = seed,
            )
        }
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
