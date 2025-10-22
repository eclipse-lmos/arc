// SPDX-FileCopyrightText: 2025 Deutsche Telekom AG and others
//
// SPDX-License-Identifier: Apache-2.0

package org.eclipse.lmos.arc.agents

import kotlinx.coroutines.coroutineScope
import org.eclipse.lmos.arc.agents.agent.Skill
import org.eclipse.lmos.arc.agents.agent.addResultTags
import org.eclipse.lmos.arc.agents.agent.agentTracer
import org.eclipse.lmos.arc.agents.agent.input
import org.eclipse.lmos.arc.agents.agent.onError
import org.eclipse.lmos.arc.agents.agent.output
import org.eclipse.lmos.arc.agents.agent.recoverAgentFailure
import org.eclipse.lmos.arc.agents.agent.spanChain
import org.eclipse.lmos.arc.agents.agent.withAgentSpan
import org.eclipse.lmos.arc.agents.conversation.AssistantMessage
import org.eclipse.lmos.arc.agents.conversation.Conversation
import org.eclipse.lmos.arc.agents.conversation.SystemMessage
import org.eclipse.lmos.arc.agents.conversation.UserMessage
import org.eclipse.lmos.arc.agents.conversation.latest
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
import org.eclipse.lmos.arc.agents.dsl.getOptional
import org.eclipse.lmos.arc.agents.dsl.provideOptional
import org.eclipse.lmos.arc.agents.dsl.setSystemPrompt
import org.eclipse.lmos.arc.agents.events.EventPublisher
import org.eclipse.lmos.arc.agents.functions.FunctionWithContext
import org.eclipse.lmos.arc.agents.functions.LLMFunction
import org.eclipse.lmos.arc.agents.functions.LLMFunctionProvider
import org.eclipse.lmos.arc.agents.functions.ListenableFunction
import org.eclipse.lmos.arc.agents.functions.NoopFunctionProvider
import org.eclipse.lmos.arc.agents.functions.toToolLoaderContext
import org.eclipse.lmos.arc.agents.llm.ChatCompleterProvider
import org.eclipse.lmos.arc.agents.llm.ChatCompletionSettings
import org.eclipse.lmos.arc.agents.llm.assignDeploymentNameOrModel
import org.eclipse.lmos.arc.agents.tracing.AgentTracer
import org.eclipse.lmos.arc.agents.tracing.GenerateResponseTagger
import org.eclipse.lmos.arc.core.Result
import org.eclipse.lmos.arc.core.failWith
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
const val AGENT_LOCAL_CONTEXT_KEY = "agent"
const val TOOLS_LOCAL_CONTEXT_KEY = "__tools__"
const val TOOL_CALLS_LOCAL_CONTEXT_KEY = "__tool_calls__"
const val AGENT_TAGS_LOCAL_CONTEXT_KEY = "agent-tags"

val ADDITIONAL_TOOL_LOCAL_CONTEXT_KEY = "${ChatAgent::class.qualifiedName}_additional_tools"

/**
 * A ChatAgent is an Agent that can interact with a user in a chat-like manner.
 */
class ChatAgent(
    override val name: String,
    override val description: String,
    override val version: String,
    override val activateOnFeatures: Set<String>?,
    private val skills: suspend () -> List<Skill>? = { null },
    private val model: suspend DSLContext.() -> String?,
    private val settings: suspend DSLContext.() -> ChatCompletionSettings?,
    private val beanProvider: BeanProvider,
    private val systemPrompt: suspend DSLContext.() -> String,
    private val toolsProvider: suspend DSLContext.() -> Unit,
    private val filterOutput: suspend OutputFilterContext.() -> Unit,
    private val filterInput: suspend InputFilterContext.() -> Unit,
    private val onFail: suspend DSLContext.(Exception) -> AssistantMessage? = { null },
    val init: DSLContext.() -> Unit,
) : ConversationAgent {

    private val log = LoggerFactory.getLogger(javaClass)

    init {
        init.invoke(BasicDSLContext(beanProvider))
    }

    override suspend fun fetchSkills() = skills.invoke()

    override suspend fun execute(input: Conversation, context: Set<Any>): Result<Conversation, AgentFailedException> {
        val compositeBeanProvider =
            CompositeBeanProvider(context + setOf(input, input.user, this).filterNotNull(), beanProvider)
        val tracer = compositeBeanProvider.agentTracer()

        return tracer.withAgentSpan(this, input) { tags, _ ->
            val agentEventHandler = beanProvider.provideOptional<EventPublisher>()
            val dslContext = BasicDSLContext(compositeBeanProvider)
            val model = model.invoke(dslContext)

            agentEventHandler?.publish(AgentStartedEvent(this@ChatAgent))
            dslContext.setLocal(AGENT_LOCAL_CONTEXT_KEY, this)
            dslContext.setLocal(AGENT_TAGS_LOCAL_CONTEXT_KEY, tags)

            var flowBreak = false
            val usedFunctions = AtomicReference<List<LLMFunction>?>(null)
            val result: Result<Conversation, AgentFailedException>
            val duration = measureTime {
                result = doExecute(input, model, dslContext, compositeBeanProvider, usedFunctions, tracer)
                    .recover { error ->
                        recoverAgentFailure(
                            error = error,
                            dslContext = dslContext,
                            input = input,
                            context = context,
                            onFail = onFail,
                        )?.let { (conversation, fb) ->
                            flowBreak = fb
                            conversation
                        }
                    }.mapFailure {
                        log.error("Agent $name failed!", it)
                        tags.onError(it)
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

            tags.addResultTags(result, flowBreak)
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

            //
            // Filter input
            //
            val filteredInput =
                tracer.spanChain("filter input", mapOf(PHASE_LOG_CONTEXT_KEY to "FilterInput")) { tags, _ ->
                    tags.input(conversation.latest<UserMessage>()?.content ?: "")
                    coroutineScope {
                        val filterContext = InputFilterContext(dslContext, conversation)
                        filterInput.invoke(filterContext).let {
                            filterContext.finish()
                            filterContext.input
                        }
                    }.also {
                        tags.output(it.latest<UserMessage>()?.content ?: "")
                    }
                }
            if (filteredInput.isEmpty()) failWith { AgentNotExecutedException("Input has been filtered") }

            //
            // Generate system prompt
            //
            val generatedSystemPrompt = tracer.spanChain(
                "generate system prompt",
                mapOf(PHASE_LOG_CONTEXT_KEY to "generatePrompt"),
            ) { tags, _ ->
                tags.input(filteredInput.latest<UserMessage>()?.content ?: "")
                systemPrompt.invoke(dslContext).also {
                    tags.output(it)
                    dslContext.setSystemPrompt(it)
                }
            }

            //
            // Load functions
            //
            val functions = functions(dslContext, compositeBeanProvider)
            usedFunctions.set(functions)
            functions?.let { dslContext.setLocal(TOOLS_LOCAL_CONTEXT_KEY, it) }

            //
            // Generate response
            //
            val fullConversation = listOf(SystemMessage(generatedSystemPrompt)) + filteredInput.transcript
            val completedConversation = tracer.spanChain(
                "generate response",
                mapOf(
                    PHASE_LOG_CONTEXT_KEY to "Generating",
                    PROMPT_LOG_CONTEXT_KEY to generatedSystemPrompt,
                    INPUT_LOG_CONTEXT_KEY to filteredInput.transcript.toLogString(),
                ),
            ) { tags, _ ->
                tags.input(filteredInput.latest<UserMessage>()?.content ?: "")
                tags.userId(conversation.user?.id ?: "")
                val completionSettings = settings.invoke(dslContext).assignDeploymentNameOrModel(model)
                val outputMessage = chatCompleter.complete(fullConversation, functions, completionSettings)
                    .getOrThrow().also { tags.outputWithUseCase(it.content) }
                outputMessage.toolCalls?.let { dslContext.setLocal(TOOL_CALLS_LOCAL_CONTEXT_KEY, it) }
                dslContext.getOptional<GenerateResponseTagger>()?.tag(tags, outputMessage, dslContext)
                conversation + outputMessage
            }

            //
            // Filter output
            //
            tracer.spanChain("filter output", mapOf(PHASE_LOG_CONTEXT_KEY to "FilterOutput")) { tags, _ ->
                tags.input(completedConversation.latest<AssistantMessage>()?.content ?: "")
                coroutineScope {
                    val filterOutputContext =
                        OutputFilterContext(dslContext, conversation, completedConversation, generatedSystemPrompt)
                    filterOutput.invoke(filterOutputContext).let {
                        filterOutputContext.finish()
                        filterOutputContext.output
                    }
                }.also {
                    tags.output(it.latest<AssistantMessage>()?.content ?: "")
                }
            }
        }

    private suspend fun BeanProvider.chatCompleter(model: String?) =
        provide(ChatCompleterProvider::class).provideByModel(model = model)

    private suspend fun functions(context: DSLContext, beanProvider: BeanProvider): List<LLMFunction>? {
        val toolsContext = ToolsDSLContext(context)
        val tools = toolsProvider.invoke(toolsContext).let { toolsContext.tools } + (
            context.getLocal(
                ADDITIONAL_TOOL_LOCAL_CONTEXT_KEY,
            ) as? Set<String>? ?: emptySet()
            )
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
        val functionProvider = beanProvider.provideOptional<LLMFunctionProvider>() ?: NoopFunctionProvider()
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
