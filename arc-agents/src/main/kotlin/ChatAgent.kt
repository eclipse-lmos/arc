// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import kotlinx.coroutines.coroutineScope
import org.eclipse.lmos.arc.agents.agent.agentTracer
import org.eclipse.lmos.arc.agents.agent.withAgentSpan
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.toLogString
import org.eclipse.lmos.arc.agents.dsl.AllTools
import org.eclipse.lmos.arc.agents.dsl.BasicDSLContext
import org.eclipse.lmos.arc.agents.dsl.BeanProvider
import org.eclipse.lmos.arc.agents.dsl.CompositeBeanProvider
import org.eclipse.lmos.arc.agents.dsl.DSLContext
import org.eclipse.lmos.arc.agents.dsl.Data
import org.eclipse.lmos.arc.agents.dsl.InputFilterContext
import org.eclipse.lmos.arc.agents.dsl.OutputFilterContext
import org.eclipse.lmos.arc.agents.dsl.ToolsDSLContext
import org.eclipse.lmos.arc.agents.dsl.addData
import org.eclipse.lmos.arc.agents.dsl.provideOptional
import org.eclipse.lmos.arc.agents.dsl.setSystemPrompt
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ListenableFunction
import org.eclipse.lmos.arc.agents.functions.toToolLoaderContext
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
import org.eclipse.lmos.arc.core.getOrNull
import org.eclipse.lmos.arc.core.getOrThrow
import org.eclipse.lmos.arc.core.mapFailure
import org.eclipse.lmos.arc.core.recover
import org.eclipse.lmos.arc.core.result
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.measureTime

const val AGENT_LOG_CONTEXT_KEY = "agent"
const val PHASE_LOG_CONTEXT_KEY = "phase"
const val PROMPT_LOG_CONTEXT_KEY = "prompt"
const val INPUT_LOG_CONTEXT_KEY = "input"

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
        val compositeBeanProvider =
            CompositeBeanProvider(context + setOf(input, input.user).filterNotNull(), beanProvider)
        val tracer = compositeBeanProvider.agentTracer()

        return tracer.withAgentSpan(name, input) { tags, _ ->
            val agentEventHandler = beanProvider.provideOptional<EventPublisher>()
            val dslContext = BasicDSLContext(compositeBeanProvider)
            val model = model.invoke(dslContext)

            agentEventHandler?.publish(AgentStartedEvent(this@ChatAgent))

            var flowBreak = false
            val usedFunctions = AtomicReference<List<LLMFunction>?>(null)
            val result: Result<Conversation, AgentFailedException>
            val duration = measureTime {
                result = doExecute(input, model, dslContext, compositeBeanProvider, usedFunctions, tracer)
                    .recover {
                        val cause = it.cause
                        when {
                            it is WithConversationResult -> {
                                log.info("Agent $name interrupted!", it)
                                flowBreak = true
                                it.conversation
                            }

                            cause is WithConversationResult -> {
                                log.info("Agent $name interrupted!", it)
                                flowBreak = true
                                cause.conversation
                            }

                            else -> null
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

            tags.tag("output.value", result.getOrNull()?.transcript?.last()?.content ?: "")
            tags.tag("output.mime_type", "text/plain")
            result
        }
    }

    private suspend fun doExecute(
        conversation: Conversation,
        model: String?,
        dslContext: DSLContext,
        compositeBeanProvider: BeanProvider,
        usedFunctions: AtomicReference<List<LLMFunction>?>,
        tracer: AgentTracer,
    ) =
        result<Conversation, Exception> {
            val chatCompleter = compositeBeanProvider.chatCompleter(model = model)

            val functions = functions(dslContext, compositeBeanProvider)
            usedFunctions.set(functions)

            val filteredInput = tracer.withSpan("filter input", mapOf(PHASE_LOG_CONTEXT_KEY to "FilterInput")) { _, _ ->
                coroutineScope {
                    val filterContext = InputFilterContext(dslContext, conversation)
                    filterInput.invoke(filterContext).let {
                        filterContext.finish()
                        filterContext.input
                    }
                }
            }

            if (filteredInput.isEmpty()) failWith { AgentNotExecutedException("Input has been filtered") }

            val generatedSystemPrompt = tracer.withSpan(
                "generate system prompt",
                mapOf(PHASE_LOG_CONTEXT_KEY to "generatePrompt"),
            ) { tags, _ ->
                systemPrompt.invoke(dslContext).also {
                    dslContext.setSystemPrompt(it)
                    tags.tag("prompt", it)
                }
            }

            val fullConversation = listOf(SystemMessage(generatedSystemPrompt)) + filteredInput.transcript

            val completedConversation = tracer.withSpan(
                "generate response",
                mapOf(
                    PHASE_LOG_CONTEXT_KEY to "Generating",
                    PROMPT_LOG_CONTEXT_KEY to generatedSystemPrompt,
                    INPUT_LOG_CONTEXT_KEY to filteredInput.transcript.toLogString(),
                ),
            ) { tags, _ ->
                conversation + chatCompleter.complete(fullConversation, functions, settings.invoke(dslContext))
                    .getOrThrow().also { tags.tag("response", it.content) }
            }

            tracer.withSpan("filter output", mapOf(PHASE_LOG_CONTEXT_KEY to "FilterOutput")) { _, _ ->
                coroutineScope {
                    val filterOutputContext =
                        OutputFilterContext(dslContext, conversation, completedConversation, generatedSystemPrompt)
                    filterOutput.invoke(filterOutputContext).let {
                        filterOutputContext.finish()
                        filterOutputContext.output
                    }
                }
            }
        }

    private suspend fun BeanProvider.chatCompleter(model: String?) =
        provide(ChatCompleterProvider::class).provideByModel(model = model)

    private suspend fun functions(context: DSLContext, beanProvider: BeanProvider): List<LLMFunction>? {
        val toolsContext = ToolsDSLContext(context)
        val tools = toolsProvider.invoke(toolsContext).let { toolsContext.tools }
        return if (tools.isNotEmpty()) {
            getFunctions(tools, beanProvider, context).map { fn ->
                if (fn is FunctionWithContext) fn.withContext(context) else fn
            }.map { fn ->
                ListenableFunction(fn) { context.addData(Data(fn.name, it)) }
            }
        } else {
            null
        }
    }

    private suspend fun getFunctions(
        tools: List<String>,
        beanProvider: BeanProvider,
        context: DSLContext,
    ): List<LLMFunction> {
        val functionProvider = beanProvider.provide(LLMFunctionProvider::class)
        val toolContext = context.toToolLoaderContext()
        return if (tools.contains(AllTools.symbol)) {
            functionProvider.provideAll(toolContext)
        } else {
            tools.map { functionProvider.provide(it, toolContext).getOrThrow() }
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
