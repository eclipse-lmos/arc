// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import kotlinx.coroutines.coroutineScope
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.dsl.*
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.core.*
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTime

const val AGENT_LOG_CONTEXT_KEY = "agent"

/**
 * A ChatAgent is an Agent that can interact with a user in a chat-like manner.
 */
class ChatAgent(
    override val name: String,
    override val description: String,
    private val model: suspend DSLContext.() -> String?,
    private val settings: suspend DSLContext.() -> ChatCompletionSettings?,
    private val beanProvider: BeanProvider,
    private val systemPrompt: suspend DSLContext.() -> String,
    private val toolsProvider: suspend DSLContext.() -> Unit,
    private val filterOutput: suspend OutputFilterContext.() -> Unit,
    private val filterInput: suspend InputFilterContext.() -> Unit,
    val init: DSLContext.() -> Unit,
) : Agent<Conversation, Conversation> {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        init.invoke(BasicDSLContext(beanProvider))
    }

    override suspend fun execute(input: Conversation, context: Set<Any>): Result<Conversation, AgentFailedException> {
        return withLogContext(mapOf(AGENT_LOG_CONTEXT_KEY to name)) {
            val agentEventHandler = beanProvider.provideOptional<EventPublisher>()
            val compositeBeanProvider =
                CompositeBeanProvider(context + setOf(input, input.user).filterNotNull(), beanProvider)
            val dslContext = BasicDSLContext(compositeBeanProvider)
            val model = model.invoke(dslContext)

            agentEventHandler?.publish(AgentStartedEvent(this@ChatAgent))

            var flowBreak = false
            val usedFunctions = AtomicReference<List<LLMFunction>?>(null)
            val result: Result<Conversation, AgentFailedException>
            val duration = measureTime {
                result = doExecute(input, model, dslContext, compositeBeanProvider, usedFunctions)
                    .recover {
                        if (it is WithConversationResult) {
                            log.info("Agent $name interrupted!", it)
                            flowBreak = true
                            it.conversation
                        } else {
                            null
                        }
                    }.mapFailure {
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
                    flowBreak = flowBreak,
                    tools = usedFunctions.get()?.map { it.name }?.toSet() ?: emptySet(),
                ),
            )
            result
        }
    }

    private suspend fun doExecute(
        conversation: Conversation,
        model: String?,
        dslContext: DSLContext,
        compositeBeanProvider: BeanProvider,
        usedFunctions: AtomicReference<List<LLMFunction>?>,
    ) =
        result<Conversation, Exception> {
            val chatCompleter = compositeBeanProvider.chatCompleter(model = model)

            val functions = functions(dslContext)
            usedFunctions.set(functions)

            val filteredInput = coroutineScope {
                val filterContext = InputFilterContext(dslContext, conversation)
                filterInput.invoke(filterContext).let {
                    filterContext.finish()
                    filterContext.input
                }
            }

            if (filteredInput.isEmpty()) failWith { AgentNotExecutedException("Input has been filtered") }

            val generatedSystemPrompt = systemPrompt.invoke(dslContext)
            val fullConversation =
                listOf(SystemMessage(generatedSystemPrompt)) + filteredInput.transcript
            val completedConversation =
                conversation + chatCompleter.complete(fullConversation, functions, settings.invoke(dslContext))
                    .getOrThrow()

            coroutineScope {
                val filterOutputContext =
                    OutputFilterContext(dslContext, conversation, completedConversation, generatedSystemPrompt)
                filterOutput.invoke(filterOutputContext).let {
                    filterOutputContext.finish()
                    filterOutputContext.output
                }
            }
        }

    private suspend fun BeanProvider.chatCompleter(model: String?) =
        provide(ChatCompleterProvider::class).provideByModel(model = model)

    private suspend fun functions(context: DSLContext): List<LLMFunction>? {
        val toolsContext = ToolsDSLContext(context)
        val tools = toolsProvider.invoke(toolsContext).let { toolsContext.tools }
        return if (tools.isNotEmpty()) {
            getFunctions(tools).map { fn ->
                if (fn is FunctionWithContext) fn.withContext(context) else fn
            }
        } else {
            null
        }
    }

    private suspend fun getFunctions(tools: List<String>): List<LLMFunction> {
        val functionProvider = beanProvider.provide(LLMFunctionProvider::class)
        return if (tools.contains(AllTools.symbol)) {
            functionProvider.provideAll()
        } else {
            tools.map { functionProvider.provide(it).getOrThrow() }
        }
    }

    override fun toString(): String {
        return "ChatAgent(name='$name', description='$description')"
    }
}

// TODO: Make the dependencies of the ChatAgent more explicit
data class ChatAgentDependencies(
    val functionProvider: LLMFunctionProvider,
    val chatCompleterProvider: ChatCompleterProvider,
    val eventPublisher: EventPublisher? = null,
)
