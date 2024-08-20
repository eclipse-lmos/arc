// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package io.github.lmos.arc.agents

import io.github.lmos.arc.agents.conversation.Conversation
import io.github.lmos.arc.agents.conversation.SystemMessage
import io.github.lmos.arc.agents.dsl.*
import io.github.lmos.arc.agents.events.EventPublisher
import io.github.lmos.arc.agents.functions.FunctionWithContext
import io.github.lmos.arc.agents.functions.LLMFunctionProvider
import io.github.lmos.arc.agents.llm.ChatCompleterProvider
import io.github.lmos.arc.agents.llm.ChatCompletionSettings
import io.github.lmos.arc.core.*
import org.slf4j.LoggerFactory
import kotlin.time.measureTime

const val AGENT_LOG_CONTEXT_KEY = "agent"

/**
 * A ChatAgent is an Agent that can interact with a user in a chat-like manner.
 */
class ChatAgent(
    override val name: String,
    override val description: String,
    private val model: () -> String?,
    private val settings: () -> ChatCompletionSettings?,
    private val beanProvider: BeanProvider,
    private val systemPrompt: suspend DSLContext.() -> String,
    private val tools: List<String>,
    private val filterOutput: suspend OutputFilterContext.() -> Unit,
    private val filterInput: suspend InputFilterContext.() -> Unit,
) : Agent<Conversation, Conversation> {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(input: Conversation, context: Set<Any>): Result<Conversation, AgentFailedException> {
        return withLogContext(mapOf(AGENT_LOG_CONTEXT_KEY to name)) {
            val agentEventHandler = beanProvider.provideOptional<EventPublisher>()
            val model = model()

            agentEventHandler?.publish(AgentStartedEvent(this@ChatAgent))
            val result: Result<Conversation, AgentFailedException>
            val duration = measureTime {
                result = doExecute(input, model, context).mapFailure {
                    log.error("Agent $name failed!", it)
                    AgentFailedException("Agent $name failed!", it)
                }
            }
            agentEventHandler?.publish(
                AgentFinishedEvent(
                    this@ChatAgent,
                    input = input,
                    output = result,
                    model = model,
                    duration = duration,
                ),
            )
            result
        }
    }

    private suspend fun doExecute(conversation: Conversation, model: String?, context: Set<Any>) =
        result<Conversation, Exception> {
            val fullContext = context + setOf(conversation, conversation.user)
            val scriptingContext = BasicDSLContext(CompositeBeanProvider(fullContext, beanProvider))
            val chatCompleter = chatCompleter(model = model)
            val functions = functions(scriptingContext)

            val generatedSystemPrompt = systemPrompt.invoke(scriptingContext)
            val filterContext = InputFilterContext(scriptingContext, conversation)
            val filteredInput = filterInput.invoke(filterContext).let { filterContext.input }

            if (filteredInput.isEmpty()) failWith { AgentNotExecutedException("Input has been filtered") }

            val fullConversation =
                listOf(SystemMessage(generatedSystemPrompt)) + filteredInput.transcript
            val completedConversation =
                conversation + chatCompleter.complete(fullConversation, functions, settings()).getOrThrow()

            val filterOutputContext =
                OutputFilterContext(scriptingContext, conversation, completedConversation, generatedSystemPrompt)
            filterOutput.invoke(filterOutputContext).let { filterOutputContext.output }
        }

    private suspend fun chatCompleter(model: String?) =
        beanProvider.provide(ChatCompleterProvider::class).provideByModel(model = model)

    private suspend fun functions(context: DSLContext) = if (tools.isNotEmpty()) {
        val functionProvider = beanProvider.provide(LLMFunctionProvider::class)
        tools.map {
            val fn = functionProvider.provide(it).getOrThrow()
            if (fn is FunctionWithContext) fn.withContext(context) else fn
        }
    } else {
        null
    }

    override fun toString(): String {
        return "ChatAgent(name='$name', description='$description')"
    }
}
