// SPDX-FileCopyrightText: 2024 Deutsche Telekom AG
//
// SPDX-License-Identifier: Apache-2.0

package ai.ancf.lmos.arc.agents

import ai.ancf.lmos.arc.agents.conversation.Conversation
import ai.ancf.lmos.arc.agents.conversation.SystemMessage
import ai.ancf.lmos.arc.agents.dsl.AllTools
import ai.ancf.lmos.arc.agents.dsl.BasicDSLContext
import ai.ancf.lmos.arc.agents.dsl.BeanProvider
import ai.ancf.lmos.arc.agents.dsl.CompositeBeanProvider
import ai.ancf.lmos.arc.agents.dsl.DSLContext
import ai.ancf.lmos.arc.agents.dsl.InputFilterContext
import ai.ancf.lmos.arc.agents.dsl.OutputFilterContext
import ai.ancf.lmos.arc.agents.dsl.ToolsDSLContext
import ai.ancf.lmos.arc.agents.dsl.provideOptional
import ai.ancf.lmos.arc.agents.events.EventPublisher
import ai.ancf.lmos.arc.agents.functions.FunctionWithContext
import ai.ancf.lmos.arc.agents.functions.LLMFunction
import ai.ancf.lmos.arc.agents.functions.LLMFunctionProvider
import ai.ancf.lmos.arc.agents.llm.ChatCompleterProvider
import ai.ancf.lmos.arc.agents.llm.ChatCompletionSettings
import ai.ancf.lmos.arc.core.Result
import ai.ancf.lmos.arc.core.failWith
import ai.ancf.lmos.arc.core.getOrThrow
import ai.ancf.lmos.arc.core.mapFailure
import ai.ancf.lmos.arc.core.recover
import ai.ancf.lmos.arc.core.result
import kotlinx.coroutines.coroutineScope
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
    private val toolsProvider: suspend DSLContext.() -> Unit,
    private val filterOutput: suspend OutputFilterContext.() -> Unit,
    private val filterInput: suspend InputFilterContext.() -> Unit,
) : Agent<Conversation, Conversation> {

    private val log = LoggerFactory.getLogger(javaClass)

    override suspend fun execute(input: Conversation, context: Set<Any>): Result<Conversation, AgentFailedException> {
        return withLogContext(mapOf(AGENT_LOG_CONTEXT_KEY to name)) {
            val agentEventHandler = beanProvider.provideOptional<EventPublisher>()
            val model = model()

            agentEventHandler?.publish(AgentStartedEvent(this@ChatAgent))
            var flowBreak = false
            val result: Result<Conversation, AgentFailedException>
            val duration = measureTime {
                result = doExecute(input, model, context)
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
                ),
            )
            result
        }
    }

    private suspend fun doExecute(conversation: Conversation, model: String?, context: Set<Any>) =
        result<Conversation, Exception> {
            val fullContext = context + setOf(conversation, conversation.user).filterNotNull()
            val compositeBeanProvider = CompositeBeanProvider(fullContext, beanProvider)
            val scriptingContext = BasicDSLContext(compositeBeanProvider)
            val chatCompleter = compositeBeanProvider.chatCompleter(model = model)
            val functions = functions(scriptingContext)

            val filteredInput = coroutineScope {
                val filterContext = InputFilterContext(scriptingContext, conversation)
                filterInput.invoke(filterContext).let {
                    filterContext.finish()
                    filterContext.input
                }
            }

            if (filteredInput.isEmpty()) failWith { AgentNotExecutedException("Input has been filtered") }

            val generatedSystemPrompt = systemPrompt.invoke(scriptingContext)
            val fullConversation =
                listOf(SystemMessage(generatedSystemPrompt)) + filteredInput.transcript
            val completedConversation =
                conversation + chatCompleter.complete(fullConversation, functions, settings()).getOrThrow()

            coroutineScope {
                val filterOutputContext =
                    OutputFilterContext(scriptingContext, conversation, completedConversation, generatedSystemPrompt)
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
